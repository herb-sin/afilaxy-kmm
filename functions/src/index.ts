import { onCall, onRequest, HttpsError } from 'firebase-functions/v2/https';
import { onDocumentCreated, onDocumentUpdated, onDocumentWritten } from 'firebase-functions/v2/firestore';
import { onSchedule } from 'firebase-functions/v2/scheduler';
import { onTaskDispatched } from 'firebase-functions/v2/tasks';
import * as admin from 'firebase-admin';
import { getFunctions } from 'firebase-admin/functions';
import * as geofireCommon from 'geofire-common';

// Inicializar Firebase Admin
admin.initializeApp();

// Stripe inicializado lazily — evita crash durante análise do módulo no deploy
let _stripe: any = null;
function getStripe() {
    if (!_stripe) {
        const key = process.env.STRIPE_SECRET_KEY;
        _stripe = require('stripe')(key);
    }
    return _stripe;
}

// ============================================
// STRIPE WEBHOOK - MVP PROFISSIONAIS
// ============================================

export const createCheckoutSession = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError('unauthenticated', 'Authentication required');
    }

    const { priceId, metadata } = request.data;

    if (!priceId || !metadata) {
        throw new HttpsError('invalid-argument', 'Missing required fields');
    }

    // Use the verified Firebase Auth token email — never trust client-supplied email.
    // A client could supply any address to associate the subscription with a different identity.
    const verifiedEmail = request.auth!.token.email;
    if (!verifiedEmail) {
        throw new HttpsError('failed-precondition', 'Email verification required');
    }

    try {
        // Usar uid do Firebase Auth como ID do documento — operação idempotente.
        // set+merge atualiza campos de perfil sem sobrescrever dados de assinatura já existentes.
        const professionalId = request.auth!.uid;
        const professionalsRef = admin.firestore().collection('health_professionals');
        const profRef = professionalsRef.doc(professionalId);

        // 1. Atualizar campos de perfil (sempre): name, email, crm
        await profRef.set(
            {
                name: metadata.name,
                email: verifiedEmail,
                crm: metadata.crm,
                specialty: 'PNEUMOLOGIST',
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            },
            { merge: true } // merge:true — não sobrescreve subscriptionPlan, subscriptionExpiry, etc.
        );

        // 2. Inicializar campos de assinatura apenas se o documento é novo (createdAt ausente)
        const profSnap = await profRef.get();
        if (!profSnap.data()?.createdAt) {
            await profRef.update({
                subscriptionPlan: 'NONE',
                subscriptionExpiry: 0,
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
            });
        }

        // 3. Criar sessão do Stripe
        const session = await getStripe().checkout.sessions.create({
            payment_method_types: ['card'],
            mode: 'subscription',
            customer_email: verifiedEmail,
            line_items: [{ price: priceId, quantity: 1 }],
            metadata: {
                professionalId: professionalId,
                planType: metadata.planType || 'BASIC',
            },
            success_url: `https://afilaxy.com/success?session_id={CHECKOUT_SESSION_ID}`,
            cancel_url: `https://afilaxy.com/cancel`,
        });

        return { sessionId: session.id, url: session.url };
    } catch (error: any) {
        console.error('Error creating checkout session:', error);
        throw new HttpsError('internal', error.message);
    }
});

export const stripeWebhook = onRequest(async (req, res) => {
    const endpointSecret = process.env.STRIPE_WEBHOOK_SECRET;
    const sig = req.headers['stripe-signature'];
    let event;
    try {
        event = getStripe().webhooks.constructEvent(req.rawBody, sig, endpointSecret);
    } catch (err: any) {
        console.error('Webhook signature verification failed:', err.message);
        res.status(400).send(`Webhook Error: ${err.message}`);
        return;
    }

    console.log('Stripe event received:', event.type);

    // Processar evento de checkout completo
    if (event.type === 'checkout.session.completed') {
        const session = event.data.object;
        const professionalId = session.metadata?.professionalId;
        const planType = session.metadata?.planType;

        if (!professionalId || !planType) {
            console.error('Missing metadata in checkout session');
            res.status(400).send('Missing metadata');
            return;
        }

        try {
            // Calcular data de expiração (30 dias)
            const expiryDate = Date.now() + (30 * 24 * 60 * 60 * 1000);

            // Atualizar Firestore
            await admin.firestore()
                .collection('health_professionals')
                .doc(professionalId)
                .update({
                    subscriptionPlan: planType.toUpperCase(),
                    subscriptionExpiry: expiryDate,
                    stripeCustomerId: session.customer,
                    stripeSubscriptionId: session.subscription,
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                });

            console.log(`✅ Subscription updated for ${professionalId}: ${planType}`);
            res.json({ received: true });
        } catch (error) {
            console.error('Error updating subscription:', error);
            res.status(500).send('Internal error');
        }
    }
    // Processar cancelamento de assinatura
    else if (event.type === 'customer.subscription.deleted') {
        const subscription = event.data.object;
        const customerId = subscription.customer;

        try {
            // Buscar profissional pelo stripeCustomerId
            const snapshot = await admin.firestore()
                .collection('health_professionals')
                .where('stripeCustomerId', '==', customerId)
                .limit(1)
                .get();

            if (!snapshot.empty) {
                const doc = snapshot.docs[0];
                await doc.ref.update({
                    subscriptionPlan: 'NONE',
                    subscriptionExpiry: 0,
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                });

                console.log(`✅ Subscription cancelled for ${doc.id}`);
            }

            res.json({ received: true });
        } catch (error) {
            console.error('Error cancelling subscription:', error);
            res.status(500).send('Internal error');
        }
    }
    // Pagamento falhou — assinatura entra em atraso antes de ser cancelada pelo Stripe.
    // Marcar como PAST_DUE imediatamente para evitar acesso indevido durante o período de retry.
    else if (event.type === 'invoice.payment_failed') {
        const invoice = event.data.object;
        const customerId = invoice.customer;
        try {
            const snapshot = await admin.firestore()
                .collection('health_professionals')
                .where('stripeCustomerId', '==', customerId)
                .limit(1)
                .get();
            if (!snapshot.empty) {
                await snapshot.docs[0].ref.update({
                    subscriptionPlan: 'PAST_DUE',
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                });
                console.log(`⚠️ Payment failed for ${snapshot.docs[0].id} — marked PAST_DUE`);
            }
            res.json({ received: true });
        } catch (error) {
            console.error('Error handling payment_failed:', error);
            res.status(500).send('Internal error');
        }
    }
    // Atualização da assinatura (renovação, downgrade, upgrade, mudança de status).
    // Sincroniza subscriptionExpiry a cada renovação bem-sucedida e atualiza status.
    else if (event.type === 'customer.subscription.updated') {
        const subscription = event.data.object;
        const customerId = subscription.customer;
        const status = subscription.status; // 'active', 'past_due', 'canceled', etc.
        const currentPeriodEnd = subscription.current_period_end * 1000; // Unix → ms
        try {
            const snapshot = await admin.firestore()
                .collection('health_professionals')
                .where('stripeCustomerId', '==', customerId)
                .limit(1)
                .get();
            if (!snapshot.empty) {
                const planStatus = status === 'active' ? undefined : 'PAST_DUE';
                const update: Record<string, any> = {
                    subscriptionExpiry: currentPeriodEnd,
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                };
                if (planStatus) update.subscriptionPlan = planStatus;
                await snapshot.docs[0].ref.update(update);
                console.log(`🔄 Subscription updated for ${snapshot.docs[0].id}: status=${status}, expiry=${new Date(currentPeriodEnd).toISOString()}`);
            }
            res.json({ received: true });
        } catch (error) {
            console.error('Error handling subscription.updated:', error);
            res.status(500).send('Internal error');
        }
    }
    else {
        console.log(`Unhandled event type: ${event.type}`);
        res.json({ received: true });
    }
});

/**
 * Cron job diário para verificar assinaturas expiradas
 * Executa todo dia às 00:00 BRT
 */
export const checkExpiredSubscriptions = onSchedule(
    { schedule: '0 0 * * *', timeZone: 'America/Sao_Paulo' },
    async () => {
        console.log('Checking expired subscriptions...');

        try {
            const now = Date.now();

            // Buscar profissionais com assinatura expirada
            const snapshot = await admin.firestore()
                .collection('health_professionals')
                .where('subscriptionExpiry', '<', now)
                .where('subscriptionPlan', '!=', 'NONE')
                .get();

            if (snapshot.empty) {
                console.log('No expired subscriptions found');
                return;
            }

            const batch = admin.firestore().batch();
            let count = 0;

            snapshot.docs.forEach((doc) => {
                batch.update(doc.ref, {
                    subscriptionPlan: 'NONE',
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                });
                count++;
                console.log(`Expiring subscription for ${doc.id}`);
            });

            await batch.commit();
            console.log(`✅ ${count} subscriptions expired`);
        } catch (error) {
            console.error('Error checking expired subscriptions:', error);
        }
    }
);

// ============================================
// FUNÇÕES EXISTENTES (EMERGÊNCIAS)
// ============================================

/**
 * Trigger quando uma emergência é criada
 * Envia notificação apenas para helpers próximos (250m)
 */
export const onEmergencyCreated = onDocumentCreated(
    'emergency_requests/{emergencyId}',
    async (event) => {
        const snap = event.data;
        if (!snap) return;
        const emergency = snap.data();
        const emergencyId = event.params.emergencyId;

        console.log(`Nova emergência criada: ${emergencyId}`);

        try {
            // Validar coordenadas da emergência
            if (!emergency.latitude || !emergency.longitude) {
                console.error('Emergência sem coordenadas válidas');
                return;
            }

            const center: [number, number] = [emergency.latitude, emergency.longitude];
            const radiusInKm = 0.25; // 250m — helper a pé chega em ~3 minutos
            const radiusInM = radiusInKm * 1000;

            console.log(`Buscando helpers dentro de ${radiusInKm}km de [${center[0]}, ${center[1]}]`);

            // Usar geohash bounds para query eficiente — só busca helpers na área correta
            const bounds = geofireCommon.geohashQueryBounds(center, radiusInM);
            console.log(`Geohash bounds: ${bounds.length} queries necessárias`);

            const boundPromises = bounds.map((b: [string, string]) =>
                admin.firestore()
                    .collection('helpers')
                    .where('isActive', '==', true)
                    .where('geohash', '>=', b[0])
                    .where('geohash', '<=', b[1])
                    .get()
            );

            const boundSnapshots = await Promise.all(boundPromises);

            // Coletar helpers únicos e filtrar por distância exata (geohash bounds são aproximados)
            const seenIds = new Set<string>();
            const nearbyHelpers: any[] = [];

            for (const snapshot of boundSnapshots) {
                for (const doc of snapshot.docs) {
                    if (seenIds.has(doc.id)) continue;
                    seenIds.add(doc.id);

                    const helper = doc.data();

                    if (helper.latitude == null || helper.longitude == null) continue;

                    const helperLocation: [number, number] = [helper.latitude, helper.longitude];
                    const distanceInKm = geofireCommon.distanceBetween(helperLocation, center);

                    console.log(`Helper ${helper.email || doc.id}: ${distanceInKm.toFixed(2)}km`);

                    // Exclui o próprio requester — um dispositivo nunca deve receber
                    // sua própria emergência, mesmo que estivesse registrado como helper.
                    if (doc.id === emergency.requesterId) {
                        console.log(`Ignorando requester ${doc.id} da lista de helpers`);
                        continue;
                    }

                    if (distanceInKm <= radiusInKm) {
                        nearbyHelpers.push({ ...helper, id: doc.id, distance: distanceInKm });
                    }
                }
            }

            if (nearbyHelpers.length === 0) {
                console.log('Nenhum helper próximo encontrado');
                return;
            }

            // Buscar tokens FCM dos helpers próximos na coleção 'users'
            const tokens: string[] = [];
            const tokenFetches = nearbyHelpers.map(async (helper) => {
                try {
                    const userDoc = await admin.firestore().collection('users').doc(helper.id).get();
                    const fcmToken = userDoc.data()?.fcmToken;
                    if (fcmToken) tokens.push(fcmToken);
                } catch (e) {
                    console.log(`Erro ao buscar token do helper ${helper.id}:`, e);
                }
            });
            await Promise.all(tokenFetches);

            if (tokens.length === 0) {
                console.log('Nenhum helper próximo com token FCM');
                return;
            }

            console.log(`✅ Notificando ${tokens.length} helper(s) dentro de ${radiusInKm}km`);

            // notification + data: o SO entrega a notificação imediatamente (sem depender do app)
            // e os extras do data chegam no intent quando o usuário clica
            const message = {
                notification: {
                    title: 'Nova Emergência de Asma',
                    body: `${emergency.requesterName || 'Alguém'} precisa de ajuda!`,
                },
                data: {
                    type: 'emergency_request',
                    emergencyId: emergencyId,
                    requesterName: emergency.requesterName || '',
                    openEmergencyResponse: 'true',
                    latitude: String(emergency.latitude || ''),
                    longitude: String(emergency.longitude || ''),
                },
                android: {
                    priority: 'high' as const,
                    notification: {
                        channelId: 'afilaxy_emergency',
                        sound: 'default',
                        defaultVibrateTimings: true,
                        priority: 'high' as const,
                    },
                },
                apns: {
                    headers: { 'apns-priority': '10' },
                    payload: {
                        aps: {
                            alert: {
                                title: 'Nova Emergência de Asma',
                                body: `${emergency.requesterName || 'Alguém'} precisa de ajuda!`,
                            },
                            sound: 'default',
                            // sem contentAvailable — notificação visível tem prioridade máxima no APNs
                        },
                    },
                },
                tokens: tokens,
            };

            const response = await admin.messaging().sendEachForMulticast(message);

            console.log(`Notificação enviada: ${response.successCount} enviadas, ${response.failureCount} falhas`);

            // Log de erros individuais
            if (response.failureCount > 0) {
                response.responses.forEach((resp, idx) => {
                    if (!resp.success) {
                        console.error(`Erro ao enviar para token ${tokens[idx]}: ${resp.error}`);
                    }
                });
            }
        } catch (error) {
            console.error('Erro ao processar emergência:', error);
        }
    }
);

/**
 * Trigger quando uma emergência é aceita
 * Envia notificação para o requester informando que ajuda está a caminho
 */
export const onEmergencyAccepted = onDocumentUpdated(
    'emergency_requests/{emergencyId}',
    async (event) => {
        const change = event.data;
        if (!change) return;
        const before = change.before.data();
        const after = change.after.data();
        const emergencyId = event.params.emergencyId;

        // Verificar se o status mudou para 'matched' (status do EmergencyRepositoryImpl.acceptEmergency)
        if (before.status !== 'matched' && after.status === 'matched') {
            console.log(`Emergência ${emergencyId} aceita`);

            try {
                // Buscar dados do requester
                const requesterDoc = await admin.firestore()
                    .collection('users')
                    .doc(after.requesterId)
                    .get();

                if (!requesterDoc.exists) {
                    console.log('Requester não encontrado');
                    return;
                }

                const requesterData = requesterDoc.data();
                const requesterToken = requesterData?.fcmToken;

                if (!requesterToken) {
                    console.log('Requester não tem token FCM');
                    return;
                }

                // Buscar dados do helper
                let helperName = 'Um ajudante';
                if (after.helperId) {
                    const helperDoc = await admin.firestore()
                        .collection('users')
                        .doc(after.helperId)
                        .get();

                    if (helperDoc.exists) {
                        const helperData = helperDoc.data();
                        helperName = helperData?.name || helperName;
                    }
                }

                // Enviar notificação
                const message = {
                    data: {
                        type: 'helper_matched',
                        emergencyId: emergencyId,
                        helperName: helperName,
                        title: 'Ajuda a Caminho!',
                        body: `${helperName} aceitou sua emergência e está indo te ajudar!`,
                    },
                    android: { priority: 'high' as const },
                    apns: {
                        headers: { 'apns-priority': '10' },
                        payload: {
                            aps: {
                                alert: {
                                    title: 'Ajuda a Caminho!',
                                    body: `${helperName} aceitou sua emergência e está indo te ajudar!`,
                                },
                                sound: 'default',
                                contentAvailable: true,
                            },
                        },
                    },
                    token: requesterToken,
                };

                await admin.messaging().send(message);
                console.log('Notificação de aceitação enviada com sucesso');
            } catch (error) {
                console.error('Erro ao enviar notificação de aceitação:', error);
            }
        }
    }
);

/**
 * Trigger quando uma mensagem é enviada no chat
 * Envia notificação para o destinatário
 */
export const onChatMessage = onDocumentCreated(
    'emergency_chats/{emergencyId}/messages/{messageId}',
    async (event) => {
        const snap = event.data;
        if (!snap) return;
        const message = snap.data();
        const emergencyId = event.params.emergencyId;

        try {
            const emergencyDoc = await admin.firestore()
                .collection('emergency_requests')
                .doc(emergencyId)
                .get();

            if (!emergencyDoc.exists) {
                console.log('Emergência não encontrada');
                return;
            }

            const emergencyData = emergencyDoc.data();

            let recipientId: string;
            if (message.senderId === emergencyData?.requesterId) {
                recipientId = emergencyData?.helperId;
            } else {
                recipientId = emergencyData?.requesterId;
            }

            if (!recipientId) {
                console.log('Destinatário não identificado');
                return;
            }

            // Buscar token FCM do destinatário
            const recipientDoc = await admin.firestore()
                .collection('users')
                .doc(recipientId)
                .get();

            if (!recipientDoc.exists) {
                console.log('Destinatário não encontrado');
                return;
            }

            const recipientData = recipientDoc.data();
            const recipientToken = recipientData?.fcmToken;

            if (!recipientToken) {
                console.log('Destinatário não tem token FCM');
                return;
            }

            const notificationMessage = {
                data: {
                    type: 'chat',
                    emergencyId: emergencyId,
                    openChat: 'true',
                    senderName: message.senderName || '',
                    message: message.message || '',
                    title: message.senderName || 'Nova mensagem',
                    body: message.message || '',
                },
                android: { priority: 'high' as const },
                apns: {
                    headers: { 'apns-priority': '10' },
                    payload: {
                        aps: {
                            alert: {
                                title: message.senderName || 'Nova mensagem',
                                body: message.message || '',
                            },
                            sound: 'default',
                            contentAvailable: true,
                        },
                    },
                },
                token: recipientToken,
            };

            await admin.messaging().send(notificationMessage);
            console.log('Notificação de chat enviada com sucesso');
        } catch (error) {
            console.error('Erro ao enviar notificação de chat:', error);
        }
    }
);

/**
 * Trigger quando localização do usuário é atualizada
 * Recalcula e salva o geohash automaticamente
 */
export const onUserLocationUpdate = onDocumentUpdated(
    'users/{userId}',
    async (event) => {
        const change = event.data;
        if (!change) return;
        const after = change.after.data();
        const before = change.before.data();

        // Verificar se localização mudou
        if (after.latitude !== before.latitude || after.longitude !== before.longitude) {
            console.log(`Atualizando geohash para usuário ${event.params.userId}`);

            try {
                // Validar coordenadas
                if (!after.latitude || !after.longitude) {
                    console.log('Coordenadas inválidas');
                    return;
                }

                // Calcular geohash
                const hash = geofireCommon.geohashForLocation([after.latitude, after.longitude]);

                // Atualizar documento com novo geohash
                await change.after.ref.update({ geohash: hash });
                console.log(`Geohash atualizado: ${hash}`);
            } catch (error) {
                console.error('Erro ao atualizar geohash:', error);
            }
        }
    }
);

// migrateHelperLocations removida — migração concluída, função era pública sem autenticação
// Para re-executar se necessário, usar Admin SDK localmente via script Node.js

/**
 * Cloud Task handler — expira uma emergência específica.
 * Enfileirada por scheduleEmergencyExpiry; a instância que criou a task já morreu.
 * Substitui o antigo setTimeout(180000) que mantinha a instância viva por 3 minutos.
 */
export const expireEmergencyTask = onTaskDispatched(
    { retryConfig: { maxAttempts: 3, minBackoffSeconds: 10 } },
    async (request) => {
        const { emergencyId } = request.data as { emergencyId: string };
        if (!emergencyId) {
            console.error('expireEmergencyTask: emergencyId ausente');
            return;
        }

        const docRef = admin.firestore().collection('emergency_requests').doc(emergencyId);
        const current = await docRef.get();

        if (!current.exists) {
            console.log(`expireEmergencyTask: ${emergencyId} não existe`);
            return;
        }

        const currentData = current.data()!;
        if (!currentData.active) {
            console.log(`Emergência ${emergencyId} já inativa — nada a fazer`);
            return;
        }

        await docRef.update({
            active: false,
            status: 'expired',
            expiredAt: admin.firestore.FieldValue.serverTimestamp()
        });

        console.log(`✅ Emergência ${emergencyId} expirada automaticamente via Cloud Task`);
    }
);

/**
 * Trigger onCreate — agenda cancelamento automático via Cloud Tasks.
 * A instância morre imediatamente após enfileirar a task (vs. setTimeout que prendia por 3 min).
 */
export const scheduleEmergencyExpiry = onDocumentCreated(
    'emergency_requests/{emergencyId}',
    async (event) => {
        const snap = event.data;
        if (!snap) return;
        const emergencyId = event.params.emergencyId;
        const data = snap.data();
        const expiresAt: number = data.expiresAt || (Date.now() + 180000);
        const delaySec = Math.max(0, Math.round((expiresAt - Date.now()) / 1000));

        console.log(`Agendando expiração de ${emergencyId} em ${delaySec}s via Cloud Task`);

        try {
            const queue = getFunctions().taskQueue('expireEmergencyTask');
            await queue.enqueue(
                { emergencyId },
                { scheduleDelaySeconds: delaySec }
            );
            console.log(`✅ Task enfileirada para ${emergencyId}`);
        } catch (error) {
            console.error(`Erro ao enfileirar task para ${emergencyId}:`, error);
            // Fallback: se Cloud Tasks falhar, garante que a emergência não fique ativa para sempre
            // A próxima leitura do cliente no app verifica expiresAt e expira localmente.
        }
    }
);

// ============================================
// AGREGAÇÃO — OPERACIONAL + GOVERNANÇA
// ============================================

/**
 * Trigger quando uma emergência é finalizada (active: true → false).
 * Agrega em:
 *   user_stats/{userId}          — operacional (app lê para alertas ao paciente)
 *   emergency_analytics/{region} — governança (dashboard de gestores)
 */
export const onEmergencyFinalized = onDocumentUpdated(
    'emergency_requests/{emergencyId}',
    async (event) => {
        const change = event.data;
        if (!change) return;
        const before = change.before.data();
        const after  = change.after.data();

        // Só processa quando active muda de true para false
        if (before.active !== true || after.active !== false) return;

        const emergencyId = event.params.emergencyId;
        const requesterId: string = after.requesterId;
        const helperId: string | undefined = after.helperId;
        const status: string = after.status || 'unknown';
        const timestamp: number = after.timestamp || Date.now();
        const resolvedAt: number | undefined = after.resolvedAt;
        const matchedAt: number | undefined = after.matchedAt;
        const lat: number = after.latitude || 0;
        const lon: number = after.longitude || 0;

        // Geohash de precisão 4 (~40km) — granularidade adequada para BI regional
        const regionHash = geofireCommon.geohashForLocation([lat, lon]).substring(0, 4);

        // Semana ISO 8601 (ex: "2026-W14")
        // Regra: semana 1 = semana que contém a primeira quinta-feira do ano (Mon–Sun).
        // Usamos UTC para evitar divergência de fuso horário entre a Cloud Function e os clientes.
        const isoDate = new Date(timestamp);
        const dayOfWeek = isoDate.getUTCDay() || 7; // converte Dom(0)→7 para Mon=1..Sun=7
        isoDate.setUTCDate(isoDate.getUTCDate() + 4 - dayOfWeek); // move para a quinta-feira da mesma semana
        const yearStart = new Date(Date.UTC(isoDate.getUTCFullYear(), 0, 1));
        const week = Math.ceil(((isoDate.getTime() - yearStart.getTime()) / 86400000 + 1) / 7);
        const weekKey = `${isoDate.getUTCFullYear()}-W${String(week).padStart(2, '0')}`;

        const db = admin.firestore();
        const inc = admin.firestore.FieldValue.increment;
        const now = admin.firestore.FieldValue.serverTimestamp;

        const batch = db.batch();

        // ── user_stats/{requesterId} ──────────────────────────────────────────
        const statsRef = db.collection('user_stats').doc(requesterId);
        const statsFlat: Record<string, any> = {
            totalEmergencies: inc(1),
            lastEmergencyAt:  timestamp,
            updatedAt:        now(),
        };
        if (status === 'resolved') statsFlat.totalResolved = inc(1);
        if (status === 'cancelled') statsFlat.totalCancelled = inc(1);
        if (status === 'expired')   statsFlat.totalExpired   = inc(1);
        if (resolvedAt && matchedAt) {
            statsFlat.totalResponseTimeSec = inc(Math.round((resolvedAt - matchedAt) / 1000));
            statsFlat.totalResponseCount   = inc(1);
        }
        const weeklyPath = new admin.firestore.FieldPath('weeklyCount', weekKey);
        batch.set(
            statsRef,
            { ...statsFlat, weeklyCount: { [weekKey]: inc(1) } },
            { mergeFields: [...Object.keys(statsFlat), weeklyPath] }
        );

        // ── emergency_analytics/{region}_{weekKey} ────────────────────────────
        const analyticsRef = db.collection('emergency_analytics').doc(`${regionHash}_${weekKey}`);
        const analyticsUpdate: Record<string, any> = {
            region:   regionHash,
            week:     weekKey,
            total:    inc(1),
            updatedAt: now(),
        };
        if (status === 'resolved')  analyticsUpdate.resolved  = inc(1);
        if (status === 'cancelled') analyticsUpdate.cancelled = inc(1);
        if (status === 'expired')   analyticsUpdate.expired   = inc(1);
        if (matchedAt) analyticsUpdate.matched = inc(1);
        if (resolvedAt && matchedAt) {
            analyticsUpdate.totalResponseTimeSec = inc(Math.round((resolvedAt - matchedAt) / 1000));
            analyticsUpdate.totalResponseCount   = inc(1);
        }
        batch.set(analyticsRef, analyticsUpdate, { merge: true });

        // ── helper_stats/{helperId} (se houve atendimento) ───────────────────
        if (helperId && status === 'resolved') {
            const helperStatsRef = db.collection('user_stats').doc(helperId);
            batch.set(helperStatsRef, {
                totalHelped:  inc(1),
                lastHelpedAt: timestamp,
                updatedAt:    now(),
            }, { merge: true });
        }

        await batch.commit();
        console.log(`✅ onEmergencyFinalized: ${emergencyId} status=${status} region=${regionHash} week=${weekKey}`);
    }
);

/**
 * Cron semanal — toda segunda-feira às 08:00 BRT.
 * Lê user_stats e envia notificação push para pacientes
 * com ≥2 emergências na semana anterior (risco elevado).
 */
export const weeklyRiskAlert = onSchedule(
    { schedule: '0 11 * * 1', timeZone: 'UTC' },
    async () => {
        const db = admin.firestore();

        // Semana anterior
        const now = new Date();
        const prevMonday = new Date(now);
        prevMonday.setDate(now.getDate() - 7);
        // Semana anterior em ISO 8601 UTC — mesma referência usada por onEmergencyFinalized.
        const isoDate = new Date(prevMonday.getTime());
        const dayOfWeek = isoDate.getUTCDay() || 7; // Dom(0)→7, Mon=1..Sun=7
        isoDate.setUTCDate(isoDate.getUTCDate() + 4 - dayOfWeek); // quinta-feira da semana
        const yearStart = new Date(Date.UTC(isoDate.getUTCFullYear(), 0, 1));
        const week = Math.ceil(((isoDate.getTime() - yearStart.getTime()) / 86400000 + 1) / 7);
        const weekKey = `${isoDate.getUTCFullYear()}-W${String(week).padStart(2, '0')}`;
        const fieldPath = `weeklyCount.${weekKey}`;

        console.log(`weeklyRiskAlert: verificando semana ${weekKey}`);

        // Pacientes com ≥2 emergências na semana
        const snapshot = await db.collection('user_stats')
            .where(fieldPath, '>=', 2)
            .get();

        if (snapshot.empty) {
            console.log('Nenhum paciente em alerta esta semana');
            return;
        }

        let sent = 0;
        for (const doc of snapshot.docs) {
            const userId = doc.id;
            const count: number = doc.data()?.weeklyCount?.[weekKey] ?? 0;

            try {
                const userDoc = await db.collection('users').doc(userId).get();
                const fcmToken: string | undefined = userDoc.data()?.fcmToken;
                if (!fcmToken) continue;

                await admin.messaging().send({
                    token: fcmToken,
                    notification: {
                        title: '⚠️ Atenção à sua saúde',
                        body: `Você teve ${count} crises esta semana. Considere consultar um especialista.`,
                    },
                    data: {
                        type: 'risk_alert',
                        weekKey,
                        count: String(count),
                    },
                });
                sent++;
            } catch (e) {
                console.error(`Erro ao notificar ${userId}:`, e);
            }
        }

        console.log(`✅ weeklyRiskAlert: ${sent} pacientes notificados (semana ${weekKey})`);
    }
);

// ============================================
// CONSULTA CRM
// ============================================

/**
 * Consulta dados de um médico pelo CRM + UF na API pública do CFM.
 * Requer autenticação — previne scraping anônimo.
 */
export const validateCrm = onCall(async (request) => {
    // Requer autenticação — previne scraping anônimo da API do CFM.
    // Qualquer usuário logado pode consultar (pacientes verificam profissionais).
    if (!request.auth) {
        throw new HttpsError('unauthenticated', 'Autenticação necessária');
    }

    const crm: string = (request.data.crm ?? '').toString().replace(/\D/g, '').slice(0, 10);
    const uf: string = (request.data.uf ?? '').toString().toUpperCase().replace(/[^A-Z]/g, '').slice(0, 2);

    if (!crm || !uf) {
        throw new HttpsError('invalid-argument', 'crm e uf são obrigatórios');
    }

    try {
        const url = `https://sistemas.cfm.org.br/api/medicos/consulta?crm=${crm}&uf=${uf}`;
        const response = await fetch(url, {
            headers: { 'Accept': 'application/json' },
            signal: AbortSignal.timeout(8000),
        });

        if (!response.ok) {
            console.warn(`CFM API retornou ${response.status} para CRM ${crm}/${uf}`);
            return { found: false };
        }

        const json: any = await response.json();
        // A API do CFM retorna array — pega o primeiro resultado
        const medico = Array.isArray(json) ? json[0] : json;

        if (!medico) return { found: false };

        return {
            found: true,
            name: medico.nome ?? medico.name ?? '',
            specialty: medico.especialidade ?? medico.specialty ?? '',
            situation: medico.situacao ?? medico.situation ?? '',
            uf: medico.uf ?? uf,
            crm: crm,
        };
    } catch (error: any) {
        console.error('Erro ao consultar CFM:', error.message);
        throw new HttpsError('unavailable', 'Serviço do CFM indisponível no momento');
    }
});

/**
 * Trigger quando um helper é ativado ou atualiza sua localização
 * Calcula e salva o geohash automaticamente na coleção 'helpers'
 * Isso permite queries eficientes por proximidade em onEmergencyCreated
 */
export const onHelperWrite = onDocumentWritten(
    'helpers/{helperId}',
    async (event) => {
        const change = event.data;
        if (!change) return;

        // Se o documento foi deletado, não fazer nada
        if (!change.after.exists) return;

        const after = change.after.data()!;
        const before = change.before.exists ? change.before.data()! : {} as any;

        // Só atualizar geohash se as coordenadas mudaram ou geohash ainda não existe
        if (after.latitude === before.latitude &&
            after.longitude === before.longitude &&
            after.geohash != null) {
            return;
        }

        if (!after.latitude || !after.longitude) {
            console.log(`Helper ${event.params.helperId} sem coordenadas válidas`);
            return;
        }

        try {
            const hash = geofireCommon.geohashForLocation([after.latitude, after.longitude]);
            await change.after.ref.update({ geohash: hash });
            console.log(`✅ Geohash do helper ${event.params.helperId} atualizado: ${hash}`);
        } catch (error) {
            console.error('Erro ao atualizar geohash do helper:', error);
        }
    }
);