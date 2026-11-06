import { onCall, onRequest, HttpsError } from 'firebase-functions/v2/https';
import { onDocumentCreated, onDocumentUpdated, onDocumentWritten } from 'firebase-functions/v2/firestore';
import { onSchedule } from 'firebase-functions/v2/scheduler';
import { onTaskDispatched } from 'firebase-functions/v2/tasks';
import { defineSecret } from 'firebase-functions/params';
import * as admin from 'firebase-admin';
import { createHmac } from 'node:crypto';
import * as nodemailer from 'nodemailer';
import { getFunctions } from 'firebase-admin/functions';
// firebase-admin v14: firestore/messaging saíram do export raiz — import dos submódulos
import { getFirestore, FieldValue, FieldPath } from 'firebase-admin/firestore';
import { getMessaging } from 'firebase-admin/messaging';
import { getStorage } from 'firebase-admin/storage';
import { getAuth } from 'firebase-admin/auth';
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
        const professionalsRef = getFirestore().collection('health_professionals');
        const profRef = professionalsRef.doc(professionalId);

        // 1. Atualizar campos de perfil (sempre): name, email, crm
        await profRef.set(
            {
                name: metadata.name,
                email: verifiedEmail,
                crm: metadata.crm,
                specialty: 'PNEUMOLOGIST',
                updatedAt: FieldValue.serverTimestamp(),
            },
            { merge: true } // merge:true — não sobrescreve subscriptionPlan, subscriptionExpiry, etc.
        );

        // 2. Inicializar campos de assinatura apenas se o documento é novo (createdAt ausente)
        const profSnap = await profRef.get();
        if (!profSnap.data()?.createdAt) {
            await profRef.update({
                subscriptionPlan: 'NONE',
                subscriptionExpiry: 0,
                createdAt: FieldValue.serverTimestamp(),
            });
        }

        // 3. Criar sessão do Stripe — plano único PARTNER
        const session = await getStripe().checkout.sessions.create({
            payment_method_types: ['card'],
            mode: 'subscription',
            customer_email: verifiedEmail,
            line_items: [{ price: priceId, quantity: 1 }],
            metadata: { professionalId: professionalId },
            success_url: `https://afilaxy.com/success?session_id={CHECKOUT_SESSION_ID}`,
            cancel_url: `https://afilaxy.com/cancel`,
        });

        return { sessionId: session.id, url: session.url };
    } catch (error: any) {
        console.error('Error creating checkout session:', error);
        throw new HttpsError('internal', 'Erro ao processar pagamento. Tente novamente.');
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

    // Checkout concluído — ativa plano PARTNER com expiração real do Stripe
    if (event.type === 'checkout.session.completed') {
        const session = event.data.object;
        const professionalId = session.metadata?.professionalId;

        if (!professionalId) {
            console.error('Missing professionalId in checkout session metadata');
            res.status(400).send('Missing metadata');
            return;
        }

        try {
            // Buscar a subscription para obter current_period_end real (evita fixar 30 dias)
            const subscription = await getStripe().subscriptions.retrieve(session.subscription);
            const expiryDate = subscription.current_period_end * 1000; // Unix → ms

            await getFirestore()
                .collection('health_professionals')
                .doc(professionalId)
                .update({
                    subscriptionPlan: 'PARTNER',
                    subscriptionExpiry: expiryDate,
                    updatedAt: FieldValue.serverTimestamp()
                });

            // stripeCustomerId/stripeSubscriptionId ficam numa coleção separada, sem regra de
            // leitura pública (cai no default-deny do firestore.rules) — health_professionals
            // é lido por qualquer usuário autenticado, e esses identificadores de billing não
            // precisam estar ali (achado de auditoria de segurança).
            await getFirestore()
                .collection('health_professionals_billing')
                .doc(professionalId)
                .set({
                    stripeCustomerId: session.customer,
                    stripeSubscriptionId: session.subscription,
                    updatedAt: FieldValue.serverTimestamp()
                }, { merge: true });

            console.log(`✅ PARTNER ativado para ${professionalId}, expira em ${new Date(expiryDate).toISOString()}`);
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
            // Buscar profissional pelo stripeCustomerId — agora em health_professionals_billing
            // (ver comentário em checkout.session.completed acima)
            const snapshot = await getFirestore()
                .collection('health_professionals_billing')
                .where('stripeCustomerId', '==', customerId)
                .limit(1)
                .get();

            if (!snapshot.empty) {
                const professionalId = snapshot.docs[0].id;
                await getFirestore().collection('health_professionals').doc(professionalId).update({
                    subscriptionPlan: 'NONE',
                    subscriptionExpiry: 0,
                    updatedAt: FieldValue.serverTimestamp()
                });

                console.log(`✅ Subscription cancelled for ${professionalId}`);
            }

            res.json({ received: true });
        } catch (error) {
            console.error('Error cancelling subscription:', error);
            res.status(500).send('Internal error');
        }
    }
    // Pagamento falhou — Stripe reprocessa automaticamente por alguns dias antes de cancelar.
    // Não alteramos o plano aqui; o acesso só é revogado em customer.subscription.deleted.
    else if (event.type === 'invoice.payment_failed') {
        const invoice = event.data.object;
        console.log(`⚠️ Pagamento falhou para customer ${invoice.customer} — Stripe vai retentar`);
        res.json({ received: true });
    }
    // Renovação ou mudança de período — sincroniza expiry com current_period_end real do Stripe.
    else if (event.type === 'customer.subscription.updated') {
        const subscription = event.data.object;
        const customerId = subscription.customer;
        const currentPeriodEnd = subscription.current_period_end * 1000; // Unix → ms
        try {
            const snapshot = await getFirestore()
                .collection('health_professionals_billing')
                .where('stripeCustomerId', '==', customerId)
                .limit(1)
                .get();
            if (!snapshot.empty) {
                const professionalId = snapshot.docs[0].id;
                await getFirestore().collection('health_professionals').doc(professionalId).update({
                    subscriptionExpiry: currentPeriodEnd,
                    updatedAt: FieldValue.serverTimestamp()
                });
                console.log(`🔄 Expiry sincronizado para ${professionalId}: ${new Date(currentPeriodEnd).toISOString()}`);
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
            const snapshot = await getFirestore()
                .collection('health_professionals')
                .where('subscriptionExpiry', '<', now)
                .where('subscriptionPlan', '!=', 'NONE')
                .get();

            if (snapshot.empty) {
                console.log('No expired subscriptions found');
                return;
            }

            const batch = getFirestore().batch();
            let count = 0;

            snapshot.docs.forEach((doc) => {
                batch.update(doc.ref, {
                    subscriptionPlan: 'NONE',
                    updatedAt: FieldValue.serverTimestamp()
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
                getFirestore()
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

                    console.log(`Helper ${doc.id}: ${distanceInKm.toFixed(2)}km`);

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

            // Token do próprio requester: um mesmo aparelho pode ter ficado registrado em
            // outra conta (troca de conta sem limpeza do token) — nunca notificar o aparelho
            // de quem pediu ajuda, mesmo que o token esteja gravado no perfil de um helper.
            const requesterDoc = await getFirestore().collection('users').doc(emergency.requesterId).get();
            const requesterToken: string | undefined = requesterDoc.data()?.fcmToken;

            // Buscar tokens FCM dos helpers próximos na coleção 'users' (Set: sem duplicatas)
            const tokenSet = new Set<string>();
            const tokenFetches = nearbyHelpers.map(async (helper) => {
                try {
                    const userDoc = await getFirestore().collection('users').doc(helper.id).get();
                    const fcmToken = userDoc.data()?.fcmToken;
                    if (fcmToken && fcmToken !== requesterToken) tokenSet.add(fcmToken);
                } catch (e) {
                    console.log(`Erro ao buscar token do helper ${helper.id}:`, e);
                }
            });
            await Promise.all(tokenFetches);
            const tokens = [...tokenSet];

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

            const response = await getMessaging().sendEachForMulticast(message);

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
                const requesterDoc = await getFirestore()
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
                    const helperDoc = await getFirestore()
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

                await getMessaging().send(message);
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
            const emergencyDoc = await getFirestore()
                .collection('emergency_requests')
                .doc(emergencyId)
                .get();

            if (!emergencyDoc.exists) {
                console.log('Emergência não encontrada');
                return;
            }

            const emergencyData = emergencyDoc.data();

            if (!emergencyData?.active) {
                console.log('Emergência já encerrada — notificação de chat suprimida');
                return;
            }

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
            const recipientDoc = await getFirestore()
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

            await getMessaging().send(notificationMessage);
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
 * Cron diário às 03:00 BRT — limpa coordenadas GPS de usuários inativos há mais de 24h.
 * Preserva dados históricos mas nulifica latitude/longitude para não expor localização stale.
 */
export const cleanStaleLocations = onSchedule(
    { schedule: '0 6 * * *', timeZone: 'UTC' },
    async () => {
        const db = getFirestore();
        const cutoff = Date.now() - 24 * 60 * 60 * 1000; // 24h atrás

        const snapshot = await db.collection('users')
            .where('locationUpdatedAt', '<', new Date(cutoff))
            .where('latitude', '!=', null)
            .get();

        if (snapshot.empty) {
            console.log('cleanStaleLocations: nenhuma localização stale encontrada');
            return;
        }

        const batch = db.batch();
        snapshot.docs.forEach((doc) => {
            batch.update(doc.ref, {
                latitude: FieldValue.delete(),
                longitude: FieldValue.delete(),
                locationUpdatedAt: FieldValue.delete(),
            });
        });

        await batch.commit();
        console.log(`✅ cleanStaleLocations: ${snapshot.size} localizações removidas`);
    }
);

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

        const docRef = getFirestore().collection('emergency_requests').doc(emergencyId);
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
            expiredAt: FieldValue.serverTimestamp()
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
        // Fallback de 3min — deve bater com EMERGENCY_TIMEOUT_MS em
        // shared/src/commonMain/kotlin/com/afilaxy/domain/model/EmergencyConstants.kt.
        // Só é usado se o documento (recém-criado) ainda não tiver expiresAt por algum motivo;
        // no fluxo normal o valor real já vem do client.
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

        const db = getFirestore();
        const inc = FieldValue.increment;
        const now = FieldValue.serverTimestamp;

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
        // weeklyCount: ISO week (legacy, kept for compatibility)
        // dailyCount: per-day count for rolling-7-day window on the client
        const dateKey = new Date(timestamp).toISOString().slice(0, 10); // "YYYY-MM-DD" UTC
        const weeklyPath = new FieldPath('weeklyCount', weekKey);
        const dailyPath  = new FieldPath('dailyCount',  dateKey);
        batch.set(
            statsRef,
            { ...statsFlat, weeklyCount: { [weekKey]: inc(1) }, dailyCount: { [dateKey]: inc(1) } },
            { mergeFields: [...Object.keys(statsFlat), weeklyPath, dailyPath] }
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
        const db = getFirestore();

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

                await getMessaging().send({
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

// ============================================
// RATE LIMITING — defesa em profundidade contra enumeração via script
// Um documento por (ação, uid) em 'rate_limits' (fechada a clients pelo
// "default deny" do firestore.rules — só o Admin SDK acessa). Combina:
//   - intervalo mínimo entre chamadas (barra loop automatizado rápido)
//   - teto de chamadas por 24h (barra abuso "devagar", ainda que espaçado)
// Uso legítimo real de getNearbyHelpers é 1 chamada por emergência criada
// (ver EmergencyViewModel.findNearbyHelpers) — um evento raro, não repetido.
// ============================================
const RATE_LIMIT_MIN_INTERVAL_MS = 5_000;
const RATE_LIMIT_MAX_PER_WINDOW = 20;
const RATE_LIMIT_WINDOW_MS = 24 * 60 * 60 * 1000;

async function enforceRateLimit(
    uid: string,
    action: string,
    maxPerWindow: number = RATE_LIMIT_MAX_PER_WINDOW,
): Promise<void> {
    const ref = getFirestore().collection('rate_limits').doc(`${action}_${uid}`);
    const now = Date.now();
    await getFirestore().runTransaction(async (tx) => {
        const snap = await tx.get(ref);
        const data = snap.data();
        const lastCallAt: number = data?.lastCallAt ?? 0;
        let windowStart: number = data?.windowStart ?? 0;
        let count: number = data?.count ?? 0;

        if (now - lastCallAt < RATE_LIMIT_MIN_INTERVAL_MS) {
            throw new HttpsError('resource-exhausted', 'Muitas chamadas em sequência. Aguarde alguns segundos.');
        }

        if (now - windowStart > RATE_LIMIT_WINDOW_MS) {
            windowStart = now;
            count = 0;
        }

        if (count >= maxPerWindow) {
            throw new HttpsError('resource-exhausted', 'Limite diário de chamadas atingido.');
        }

        tx.set(ref, { lastCallAt: now, windowStart, count: count + 1 });
    });
}

// ============================================
// NEARBY HELPERS — Mitigação R1 de segurança
// Substitui a leitura direta do cliente na coleção 'helpers'.
// O Admin SDK bypassa regras Firestore, permitindo fechar
// o allow read público na coleção helpers.
//
// Achado #4 da auditoria (2026-09): o único uso legítimo restante é
// EmergencyViewModel.findNearbyHelpers(radiusKm = 0.25), chamado uma vez
// ao criar uma emergência. O recurso de "mapa" com raio de 5km/10km era
// código morto (sem consumidor em nenhuma UI) e foi removido do app —
// por isso o raio abaixo é travado em 250m, ignorando qualquer valor
// vindo do cliente, e a chamada passa por rate limiting.
// ============================================
export const getNearbyHelpers = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError('unauthenticated', 'Authentication required');
    }

    const callerUid = request.auth.uid;
    await enforceRateLimit(callerUid, 'getNearbyHelpers');

    const lat = Number(request.data.latitude);
    const lon = Number(request.data.longitude);
    // Raio fixo em 250m — ver comentário acima. Não aceita mais radiusKm do cliente.
    // Deve bater com HELPER_RADIUS_KM em shared/.../EmergencyViewModel.kt e com o default em
    // AfilaxyApp.swift (iOS) — sem constante compartilhada entre as 3 linguagens, mantido
    // manualmente em sincronia.
    const radiusKm = 0.25;

    if (isNaN(lat) || isNaN(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
        throw new HttpsError('invalid-argument', 'Coordenadas inválidas');
    }

    const center: [number, number] = [lat, lon];
    const radiusInM = radiusKm * 1000;

    const bounds = geofireCommon.geohashQueryBounds(center, radiusInM);
    const boundPromises = bounds.map((b: [string, string]) =>
        getFirestore()
            .collection('helpers')
            .where('isActive', '==', true)
            .where('geohash', '>=', b[0])
            .where('geohash', '<=', b[1])
            .get()
    );

    const snapshots = await Promise.all(boundPromises);
    const seen = new Set<string>();
    const helpers: any[] = [];

    for (const snap of snapshots) {
        for (const doc of snap.docs) {
            if (seen.has(doc.id)) continue;
            seen.add(doc.id);
            if (doc.id === callerUid) continue; // nunca retorna o próprio caller

            const data = doc.data();
            if (data.latitude == null || data.longitude == null) continue;

            const helperLoc: [number, number] = [data.latitude, data.longitude];
            const dist = geofireCommon.distanceBetween(helperLoc, center);
            if (dist > radiusKm) continue;

            helpers.push({
                id: doc.id,
                name: data.name ?? 'Helper',
                latitude: data.latitude,
                longitude: data.longitude,
                distance: dist,
            });
        }
    }

    return { helpers: helpers.sort((a, b) => a.distance - b.distance) };
});

/**
 * Trigger quando um helper é ativado ou atualiza sua localização
 * Calcula e salva o geohash automaticamente na coleção 'helpers'
 * Isso permite queries eficientes por proximidade em onEmergencyCreated
 */
// ============================================
// EMERGENCY_PINGS — Mitigação de segurança (achado #2 da auditoria)
// Espelha em emergency_requests apenas os campos não identificáveis (sem
// requesterName, sem GPS exato) para uma coleção que qualquer usuário
// autenticado pode listar/ler com segurança. emergency_requests em si passa
// a ser list/get apenas para participantes (requesterId/helperId).
// ============================================
export const onEmergencyRequestWrite = onDocumentWritten(
    'emergency_requests/{emergencyId}',
    async (event) => {
        const change = event.data;
        if (!change) return;

        const emergencyId = event.params.emergencyId;
        const pingRef = getFirestore().collection('emergency_pings').doc(emergencyId);

        // emergency_requests nunca é deletado via regras (allow delete: if false),
        // mas mantemos o espelho consistente caso isso mude ou ocorra via console.
        if (!change.after.exists) {
            await pingRef.delete().catch(() => {});
            return;
        }

        const after = change.after.data()!;

        if (after.latitude == null || after.longitude == null) {
            console.error(`emergency_requests/${emergencyId} sem coordenadas válidas — ping não espelhado`);
            return;
        }

        // Arredonda a ~0.001° (~111m) — mesma obfuscação já usada em activateHelper()
        // no app. Suficiente para a query de proximidade; não localiza um endereço.
        const roundedLat = Math.round(after.latitude * 1000) / 1000;
        const roundedLon = Math.round(after.longitude * 1000) / 1000;

        try {
            await pingRef.set({
                active: after.active === true,
                status: after.status ?? 'waiting',
                requesterId: after.requesterId ?? null,
                helperId: after.helperId ?? null,
                latitude: roundedLat,
                longitude: roundedLon,
                timestamp: after.timestamp ?? null,
                // Não é PII — necessário para o countdown em EmergencyResponseScreen, que lê
                // esta projeção antes do helper aceitar (e virar participante de emergency_requests).
                expiresAt: after.expiresAt ?? null,
            });
        } catch (error) {
            console.error(`Erro ao espelhar emergency_pings/${emergencyId}:`, error);
        }
    }
);

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
// ============================================
// LEADS DO SITE (afilaxy.com/profissionais)
// Substitui a gravação direta do site em 'leads' (que exigia login anônimo e
// App Check/reCAPTCHA — os dois derrubaram o formulário sem ninguém perceber).
// Aqui o servidor valida, limita abuso por IP, grava com o Admin SDK e avisa
// por e-mail — se o formulário parar de chegar, a falta dos avisos denuncia.
// ============================================
const LEAD_NOTIFY_ADDRESS = 'afilaxy@gmail.com';
// Senha de app do Gmail de afilaxy@gmail.com (não a senha da conta).
// Configurar com: firebase functions:secrets:set GMAIL_APP_PASSWORD
const gmailAppPassword = defineSecret('GMAIL_APP_PASSWORD');
const LEAD_MAX_PER_DAY_PER_IP = 5;

function requireText(value: unknown, field: string, min: number, max: number): string {
    const text = typeof value === 'string' ? value.trim() : '';
    if (text.length < min || text.length > max) {
        throw new HttpsError('invalid-argument', `Campo inválido: ${field}`);
    }
    return text;
}

function optionalText(value: unknown, max: number): string {
    return typeof value === 'string' ? value.trim().slice(0, max) : '';
}

export const submitLead = onCall(
    {
        secrets: [gmailAppPassword],
        // Só o site chama esta função — outros domínios recebem erro de CORS no navegador.
        cors: ['https://afilaxy.com', 'https://www.afilaxy.com', 'http://localhost:5173'],
    },
    async (request) => {
        const data = request.data ?? {};
        const lead = {
            name: requireText(data.name, 'nome', 2, 200),
            email: requireText(data.email, 'e-mail', 5, 100),
            phone: optionalText(data.phone, 30),
            crm: optionalText(data.crm, 20),
            cnpj: optionalText(data.cnpj, 20),
            plan: requireText(data.plan, 'plano', 1, 200),
            type: data.type === 'clinica' ? 'clinica' : 'medico',
            segment: optionalText(data.segment, 100),
        };
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(lead.email)) {
            throw new HttpsError('invalid-argument', 'Campo inválido: e-mail');
        }

        // Limite por IP sem guardar o IP: HMAC com um segredo do servidor, então o
        // valor em 'rate_limits' não pode ser revertido para o IP real (LGPD).
        // Atrás do balanceador do Google, o IP do visitante é o primeiro de X-Forwarded-For.
        const forwarded = request.rawRequest.headers['x-forwarded-for'];
        const ip = (Array.isArray(forwarded) ? forwarded[0] : forwarded)?.split(',')[0]?.trim()
            || request.rawRequest.ip
            || 'desconhecido';
        const ipKey = createHmac('sha256', gmailAppPassword.value()).update(ip).digest('hex').slice(0, 32);
        await enforceRateLimit(ipKey, 'submitLead', LEAD_MAX_PER_DAY_PER_IP);

        const ref = await getFirestore().collection('leads').add({
            ...lead,
            source: 'site',
            createdAt: FieldValue.serverTimestamp(),
        });

        // Falha no e-mail não pode perder o lead — ele já está gravado.
        try {
            const transporter = nodemailer.createTransport({
                service: 'gmail',
                auth: { user: LEAD_NOTIFY_ADDRESS, pass: gmailAppPassword.value() },
            });
            await transporter.sendMail({
                from: `Afilaxy <${LEAD_NOTIFY_ADDRESS}>`,
                to: LEAD_NOTIFY_ADDRESS,
                replyTo: lead.email,
                subject: `Novo interessado: ${lead.name} (${lead.plan})`,
                text: [
                    `Nome: ${lead.name}`,
                    `E-mail: ${lead.email}`,
                    `Telefone: ${lead.phone || '-'}`,
                    `CRM: ${lead.crm || '-'}`,
                    `CNPJ: ${lead.cnpj || '-'}`,
                    `Plano: ${lead.plan}`,
                    `Tipo: ${lead.type}`,
                    `Segmento: ${lead.segment || '-'}`,
                    '',
                    `Documento: leads/${ref.id}`,
                ].join('\n'),
            });
        } catch (error) {
            console.error(`submitLead: lead ${ref.id} gravado, mas o e-mail de aviso falhou:`, error);
        }

        return { ok: true };
    }
);

// ============================================
// FCM TOKEN ÚNICO POR APARELHO
// O token de notificação fica num campo único users/{uid}.fcmToken e o logout não o
// apagava. Ao trocar de conta no mesmo aparelho, as duas contas ficavam com o mesmo
// token — e as notificações de uma (com o nome de quem pede ajuda) iam para o aparelho
// da outra. Sempre que um perfil passa a ter um token, ele é removido dos demais.
// Server-side de propósito: corrige também as versões do app já instaladas.
// ============================================
export const onUserFcmTokenWrite = onDocumentWritten(
    'users/{userId}',
    async (event) => {
        const before = event.data?.before?.data();
        const after = event.data?.after?.data();
        const token: string | undefined = after?.fcmToken;
        if (!token || token === before?.fcmToken) return;

        const userId = event.params.userId;
        const others = await getFirestore()
            .collection('users')
            .where('fcmToken', '==', token)
            .get();

        const batch = getFirestore().batch();
        let removed = 0;
        for (const doc of others.docs) {
            if (doc.id === userId) continue;
            batch.update(doc.ref, { fcmToken: FieldValue.delete() });
            removed++;
        }
        if (removed > 0) {
            await batch.commit();
            console.log(`onUserFcmTokenWrite: token de ${userId} removido de ${removed} outro(s) perfil(is)`);
        }
    }
);

// ============================================
// EXCLUSÃO DE CONTA (LGPD art. 18, VI; Google Play e App Store exigem no app)
// Apaga, em cascata, tudo o que pertence ao usuário e por último a conta do
// Firebase Authentication. É idempotente: se falhar no meio, o usuário ainda
// está logado e pode tentar de novo; o que já foi apagado só não é achado.
//
// Emergências:
//   - em que o usuário pediu ajuda → o pedido e o chat inteiro são apagados
//     (delete direto, sem passar por active=false, para o onEmergencyFinalized
//     não recriar user_stats do usuário que está sendo apagado);
//   - em que o usuário atendeu → o registro fica (é histórico de quem pediu
//     ajuda), sem helperId/helperName, e as mensagens dele no chat são apagadas.
//     Se ainda estava ativa, é cancelada.
// Assinatura de profissional: cancelada no Stripe ANTES de apagar qualquer
// coisa — se o cancelamento falhar, nada é apagado (a pessoa não pode continuar
// sendo cobrada sem ter mais a conta).
// Sign in with Apple: a revogação do token (exigência da App Store) é feita
// no app iOS antes de chamar esta função — precisa de um authorizationCode novo.
// ============================================

// Coleções em que o documento pertence ao usuário pelo campo indicado.
const OWNED_BY_FIELD: Array<[collection: string, field: string]> = [
    ['medications', 'userId'],
    ['emergency_contacts', 'userId'],
    ['medical_exams', 'userId'],
    ['notifications', 'userId'],
    ['nps_responses', 'userId'],
    ['consultation_interest', 'userId'],
    ['ratings', 'userId'],
    ['ratings', 'reviewedId'],
    ['reviews', 'reviewerId'],
    ['reviews', 'reviewedId'],
];

// Documentos com id = uid (recursiveDelete também apaga as subcoleções:
// checkins/{uid}/responses, act/{uid}/results, risk_scores/{uid}/snapshots).
const OWNED_BY_ID = [
    'users', 'helpers', 'medical_profiles', 'checkins', 'act', 'risk_scores',
    'user_stats', 'health_professionals', 'health_professionals_billing',
];

// Prefixos do Storage com arquivos do usuário (ver storage.rules).
const OWNED_STORAGE_PREFIXES = (uid: string) => [`users/${uid}/`, `professionals/${uid}/`];

async function cancelStripeSubscriptionIfAny(uid: string): Promise<void> {
    const billing = await getFirestore().collection('health_professionals_billing').doc(uid).get();
    const subscriptionId: string | undefined = billing.data()?.stripeSubscriptionId;
    if (!subscriptionId) return;
    try {
        // Numa nova tentativa (exclusão que falhou depois do cancelamento) a assinatura
        // já está cancelada — não cancela de novo.
        const subscription = await getStripe().subscriptions.retrieve(subscriptionId);
        if (subscription.status === 'canceled') return;
        await getStripe().subscriptions.cancel(subscriptionId);
    } catch (error: any) {
        // Já cancelada ou inexistente no Stripe: nada a cobrar, segue a exclusão.
        if (error?.code === 'resource_missing') return;
        console.error(`deleteAccount: falha ao cancelar assinatura de ${uid}:`, error?.message);
        throw new HttpsError('unavailable',
            'Não foi possível cancelar sua assinatura agora. Nada foi apagado. Tente novamente em alguns minutos.');
    }
}

// Aplica exclusões e atualizações em lotes (limite do Firestore: 500 por lote).
// Diferente do BulkWriter, uma falha interrompe a exclusão com erro — a conta de
// login só é apagada se todos os dados tiverem saído.
async function commitInBatches(
    deletes: Map<string, FirebaseFirestore.DocumentReference>,
    updates: Array<[FirebaseFirestore.DocumentReference, Record<string, any>]>,
): Promise<void> {
    const db = getFirestore();
    const ops: Array<(b: FirebaseFirestore.WriteBatch) => void> = [
        ...updates.map(([ref, data]) => (b: FirebaseFirestore.WriteBatch) => { b.update(ref, data); }),
        ...[...deletes.values()].map((ref) => (b: FirebaseFirestore.WriteBatch) => { b.delete(ref); }),
    ];
    for (let i = 0; i < ops.length; i += 400) {
        const batch = db.batch();
        ops.slice(i, i + 400).forEach((op) => op(batch));
        await batch.commit();
    }
}

export const deleteAccount = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError('unauthenticated', 'Autenticação necessária');
    }
    const uid = request.auth.uid;
    const db = getFirestore();

    await cancelStripeSubscriptionIfAny(uid);

    // Map por caminho: o mesmo documento pode aparecer em mais de uma consulta
    // (ex.: avaliação com userId e reviewedId iguais) e não pode ser escrito 2x.
    const deletes = new Map<string, FirebaseFirestore.DocumentReference>();
    const markDelete = (ref: FirebaseFirestore.DocumentReference) => deletes.set(ref.path, ref);
    const updates: Array<[FirebaseFirestore.DocumentReference, Record<string, any>]> = [];
    const emergencies = db.collection('emergency_requests');

    // 1. Emergências em que o usuário pediu ajuda: pedido + chat inteiro.
    const asRequester = await emergencies.where('requesterId', '==', uid).get();
    for (const doc of asRequester.docs) {
        await db.recursiveDelete(db.collection('emergency_chats').doc(doc.id));
        markDelete(doc.ref);
    }

    // 2. Emergências em que o usuário atendeu: anonimiza e apaga só as mensagens dele.
    //    Pula as que ele mesmo pediu (o iOS antigo deixava aceitar o próprio pedido) —
    //    essas já saem inteiras no passo 1.
    const asHelper = await emergencies.where('helperId', '==', uid).get();
    let anonymized = 0;
    for (const doc of asHelper.docs) {
        if (doc.data().requesterId === uid) continue;
        const update: Record<string, any> = {
            helperId: FieldValue.delete(),
            helperName: 'Conta excluída',
        };
        if (doc.data().active === true) {
            update.active = false;
            update.status = 'cancelled';
        }
        updates.push([doc.ref, update]);
        anonymized++;

        const ownMessages = await db.collection('emergency_chats').doc(doc.id)
            .collection('messages').where('senderId', '==', uid).get();
        ownMessages.docs.forEach((m) => markDelete(m.ref));
    }

    // 3. Documentos ligados ao usuário por campo.
    for (const [collection, field] of OWNED_BY_FIELD) {
        const snap = await db.collection(collection).where(field, '==', uid).get();
        snap.docs.forEach((d) => markDelete(d.ref));
    }
    markDelete(db.collection('rate_limits').doc(`getNearbyHelpers_${uid}`));

    await commitInBatches(deletes, updates);

    // 4. Documentos com id = uid, com subcoleções.
    for (const collection of OWNED_BY_ID) {
        await db.recursiveDelete(db.collection(collection).doc(uid));
    }

    // 5. Arquivos (fotos de perfil).
    const bucket = getStorage().bucket();
    for (const prefix of OWNED_STORAGE_PREFIXES(uid)) {
        await bucket.deleteFiles({ prefix });
    }

    // 6. Por último, a conta de login — antes disso o usuário ainda pode tentar de novo.
    await getAuth().deleteUser(uid);

    console.log(`deleteAccount: conta ${uid} excluída (${asRequester.size} pedido(s), ${anonymized} atendimento(s) anonimizado(s))`);
    return { deleted: true };
});
