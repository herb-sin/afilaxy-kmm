import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import * as geofireCommon from 'geofire-common';

// Inicializar Firebase Admin
admin.initializeApp();

// Stripe inicializado uma vez (evita re-instanciação a cada cold start)
const stripe = require('stripe')(functions.config().stripe.secret_key);

// ============================================
// STRIPE WEBHOOK - MVP PROFISSIONAIS
// ============================================

export const createCheckoutSession = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
    }
    
    const { priceId, email, metadata } = data;

    if (!priceId || !email || !metadata) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing required fields');
    }

    try {
        // Criar ou buscar profissional no Firestore
        const professionalsRef = admin.firestore().collection('health_professionals');
        const existingProf = await professionalsRef.where('email', '==', email).limit(1).get();
        
        let professionalId: string;
        
        if (existingProf.empty) {
            // Criar novo profissional
            const newProf = await professionalsRef.add({
                name: metadata.name,
                email: email,
                crm: metadata.crm,
                specialty: 'PNEUMOLOGIST', // Padrão, pode ser atualizado depois
                subscriptionPlan: 'NONE',
                subscriptionExpiry: 0,
                createdAt: admin.firestore.FieldValue.serverTimestamp()
            });
            professionalId = newProf.id;
        } else {
            professionalId = existingProf.docs[0].id;
        }

        // Criar sessão de checkout no Stripe
        const session = await stripe.checkout.sessions.create({
            payment_method_types: ['card'],
            line_items: [{
                price: priceId,
                quantity: 1
            }],
            mode: 'subscription',
            success_url: `${functions.config().app?.url || 'https://afilaxy.com'}/professional/success?session_id={CHECKOUT_SESSION_ID}`,
            cancel_url: `${functions.config().app?.url || 'https://afilaxy.com'}/professional/cancel`,
            customer_email: email,
            metadata: {
                professionalId,
                planType: metadata.planType,
                name: metadata.name,
                crm: metadata.crm
            }
        });

        console.log(`✅ Checkout session created for ${email}: ${session.id}`);

        return { sessionId: session.id };
    } catch (error: any) {
        console.error('Error creating checkout session:', error);
        throw new functions.https.HttpsError('internal', error.message);
    }
});

/**
 * Webhook do Stripe para processar eventos de pagamento
 * Atualiza subscriptionPlan e subscriptionExpiry no Firestore
 */
export const stripeWebhook = functions.https.onRequest(async (req, res) => {
    const endpointSecret = functions.config().stripe.webhook_secret;

    const sig = req.headers['stripe-signature'];

    let event;

    try {
        event = stripe.webhooks.constructEvent(req.rawBody, sig, endpointSecret);
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
    else {
        console.log(`Unhandled event type: ${event.type}`);
        res.json({ received: true });
    }
});

/**
 * Cron job diário para verificar assinaturas expiradas
 * Executa todo dia às 00:00 UTC
 */
export const checkExpiredSubscriptions = functions.pubsub
    .schedule('0 0 * * *')
    .timeZone('America/Sao_Paulo')
    .onRun(async (context) => {
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
                return null;
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

            return null;
        } catch (error) {
            console.error('Error checking expired subscriptions:', error);
            return null;
        }
    });

// ============================================
// FUNÇÕES EXISTENTES (EMERGÊNCIAS)
// ============================================

/**
 * Trigger quando uma emergência é criada
 * Envia notificação apenas para helpers próximos (5km)
 */
export const onEmergencyCreated = functions.firestore
    .document('emergencies/{emergencyId}')
    .onCreate(async (snap, context) => {
        const emergency = snap.data();
        const emergencyId = context.params.emergencyId;

        console.log(`Nova emergência criada: ${emergencyId}`);

        try {
            // Validar coordenadas da emergência
            if (!emergency.latitude || !emergency.longitude) {
                console.error('Emergência sem coordenadas válidas');
                return null;
            }

            const center: [number, number] = [emergency.latitude, emergency.longitude];
            const radiusInM = 5000; // 5km

            console.log(`Buscando helpers dentro de ${radiusInM / 1000}km de [${center[0]}, ${center[1]}]`);

            // Calcular bounds do geohash para query eficiente
            const bounds = geofireCommon.geohashQueryBounds(center, radiusInM);
            console.log(`Geohash bounds: ${bounds.length} queries necessárias`);

            // Executar queries em paralelo para cada bound
            const promises = bounds.map((b: [string, string]) => {
                return admin.firestore()
                    .collection('users')
                    .where('isHelper', '==', true)
                    .where('geohash', '>=', b[0])
                    .where('geohash', '<=', b[1])
                    .get();
            });

            const snapshots = await Promise.all(promises);

            // Coletar helpers únicos e filtrar por distância exata
            const helpersMap = new Map<string, any>();
            const nearbyHelpers: any[] = [];

            for (const snapshot of snapshots) {
                for (const doc of snapshot.docs) {
                    // Evitar duplicatas
                    if (helpersMap.has(doc.id)) continue;

                    const helper = doc.data();
                    helpersMap.set(doc.id, helper);

                    // Verificar se tem coordenadas
                    if (!helper.latitude || !helper.longitude) {
                        console.log(`Helper ${doc.id} sem coordenadas, ignorado`);
                        continue;
                    }

                    // Calcular distância exata
                    const helperLocation: [number, number] = [helper.latitude, helper.longitude];
                    const distanceInKm = geofireCommon.distanceBetween(helperLocation, center);

                    console.log(`Helper ${helper.name || doc.id}: ${distanceInKm.toFixed(2)}km de distância`);

                    // Filtrar por raio
                    if (distanceInKm <= radiusInM / 1000) {
                        nearbyHelpers.push({
                            ...helper,
                            id: doc.id,
                            distance: distanceInKm
                        });
                    }
                }
            }

            if (nearbyHelpers.length === 0) {
                console.log('Nenhum helper próximo encontrado');
                return null;
            }

            // Coletar tokens FCM
            const tokens: string[] = [];
            nearbyHelpers.forEach((helper) => {
                if (helper.fcmToken) {
                    tokens.push(helper.fcmToken);
                }
            });

            if (tokens.length === 0) {
                console.log('Nenhum helper próximo com token FCM');
                return null;
            }

            console.log(`✅ Notificando ${tokens.length} helper(s) dentro de ${radiusInM / 1000}km`);

            // Enviar notificação multicast
            const message = {
                notification: {
                    title: '🆘 Nova Emergência',
                    body: `${emergency.requesterName || 'Alguém'} precisa de ajuda!`,
                },
                data: {
                    type: 'emergency',
                    emergencyId: emergencyId,
                    requesterName: emergency.requesterName || '',
                    latitude: String(emergency.latitude || ''),
                    longitude: String(emergency.longitude || ''),
                },
                tokens: tokens,
            };

            const response = await admin.messaging().sendMulticast(message);

            console.log(`Notificação enviada: ${response.successCount} enviadas, ${response.failureCount} falhas`);

            // Log de erros
            if (response.failureCount > 0) {
                response.responses.forEach((resp, idx) => {
                    if (!resp.success) {
                        console.error(`Erro ao enviar para token ${tokens[idx]}: ${resp.error}`);
                    }
                });
            }

            return null;
        } catch (error) {
            console.error('Erro ao processar emergência:', error);
            return null;
        }
    });

/**
 * Trigger quando uma emergência é aceita
 * Envia notificação para o requester informando que ajuda está a caminho
 */
export const onEmergencyAccepted = functions.firestore
    .document('emergencies/{emergencyId}')
    .onUpdate(async (change, context) => {
        const before = change.before.data();
        const after = change.after.data();
        const emergencyId = context.params.emergencyId;

        // Verificar se o status mudou para 'accepted'
        if (before.status !== 'accepted' && after.status === 'accepted') {
            console.log(`Emergência ${emergencyId} aceita`);

            try {
                // Buscar dados do requester
                const requesterDoc = await admin.firestore()
                    .collection('users')
                    .doc(after.requesterId)
                    .get();

                if (!requesterDoc.exists) {
                    console.log('Requester não encontrado');
                    return null;
                }

                const requesterData = requesterDoc.data();
                const requesterToken = requesterData?.fcmToken;

                if (!requesterToken) {
                    console.log('Requester não tem token FCM');
                    return null;
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
                    notification: {
                        title: '✅ Ajuda a Caminho!',
                        body: `${helperName} aceitou sua emergência e está indo te ajudar!`,
                    },
                    data: {
                        type: 'emergency_accepted',
                        emergencyId: emergencyId,
                        helperName: helperName,
                    },
                    token: requesterToken,
                };

                await admin.messaging().send(message);
                console.log('Notificação de aceitação enviada com sucesso');

                return null;
            } catch (error) {
                console.error('Erro ao enviar notificação de aceitação:', error);
                return null;
            }
        }

        return null;
    });

/**
 * Trigger quando uma mensagem é enviada no chat
 * Envia notificação para o destinatário
 */
export const onChatMessage = functions.firestore
    .document('emergency_chats/{emergencyId}/messages/{messageId}')
    .onCreate(async (snap, context) => {
        const message = snap.data();
        const emergencyId = context.params.emergencyId;

        try {
            const emergencyDoc = await admin.firestore()
                .collection('emergency_requests')
                .doc(emergencyId)
                .get();

            if (!emergencyDoc.exists) {
                console.log('Emergência não encontrada');
                return null;
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
                return null;
            }

            // Buscar token FCM do destinatário
            const recipientDoc = await admin.firestore()
                .collection('users')
                .doc(recipientId)
                .get();

            if (!recipientDoc.exists) {
                console.log('Destinatário não encontrado');
                return null;
            }

            const recipientData = recipientDoc.data();
            const recipientToken = recipientData?.fcmToken;

            if (!recipientToken) {
                console.log('Destinatário não tem token FCM');
                return null;
            }

            const notificationMessage = {
                notification: {
                    title: message.senderName || 'Nova mensagem',
                    body: message.message || '',
                },
                data: {
                    type: 'chat',
                    emergencyId: emergencyId,
                    senderName: message.senderName || '',
                    message: message.message || '',
                },
                token: recipientToken,
            };

            await admin.messaging().send(notificationMessage);
            console.log('Notificação de chat enviada com sucesso');

            return null;
        } catch (error) {
            console.error('Erro ao enviar notificação de chat:', error);
            return null;
        }
    });

/**
 * Trigger quando localização do usuário é atualizada
 * Recalcula e salva o geohash automaticamente
 */
export const onUserLocationUpdate = functions.firestore
    .document('users/{userId}')
    .onUpdate(async (change, context) => {
        const after = change.after.data();
        const before = change.before.data();

        // Verificar se localização mudou
        if (after.latitude !== before.latitude || after.longitude !== before.longitude) {
            console.log(`Atualizando geohash para usuário ${context.params.userId}`);

            try {
                // Validar coordenadas
                if (!after.latitude || !after.longitude) {
                    console.log('Coordenadas inválidas');
                    return null;
                }

                // Calcular geohash
                const hash = geofireCommon.geohashForLocation([after.latitude, after.longitude]);

                // Atualizar documento com novo geohash
                await change.after.ref.update({ geohash: hash });
                console.log(`Geohash atualizado: ${hash}`);

                return null;
            } catch (error) {
                console.error('Erro ao atualizar geohash:', error);
                return null;
            }
        }

        return null;
    });

// migrateHelperLocations removida — migração concluída, função era pública sem autenticação
// Para re-executar se necessário, usar Admin SDK localmente via script Node.js