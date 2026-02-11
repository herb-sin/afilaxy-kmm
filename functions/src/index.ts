import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import * as geofireCommon from 'geofire-common';

// Inicializar Firebase Admin
admin.initializeApp();

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
    .document('emergencies/{emergencyId}/messages/{messageId}')
    .onCreate(async (snap, context) => {
        const message = snap.data();
        const emergencyId = context.params.emergencyId;

        console.log(`Nova mensagem no chat da emergência ${emergencyId}`);

        try {
            // Buscar dados da emergência
            const emergencyDoc = await admin.firestore()
                .collection('emergencies')
                .doc(emergencyId)
                .get();

            if (!emergencyDoc.exists) {
                console.log('Emergência não encontrada');
                return null;
            }

            const emergencyData = emergencyDoc.data();

            // Determinar destinatário (se sender é requester, enviar para helper e vice-versa)
            let recipientId: string;
            if (message.senderId === emergencyData?.requesterId) {
                // Mensagem do requester, enviar para helper
                recipientId = emergencyData?.helperId;
            } else {
                // Mensagem do helper, enviar para requester
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

            // Enviar notificação
            const notificationMessage = {
                notification: {
                    title: message.senderName || 'Nova mensagem',
                    body: message.text || '',
                },
                data: {
                    type: 'chat',
                    emergencyId: emergencyId,
                    senderName: message.senderName || '',
                    message: message.text || '',
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

/**
 * HTTP Function para migrar dados de helpers existentes
 * Execute uma vez: curl https://us-central1-afilaxy-app.cloudfunctions.net/migrateHelperLocations
 */
export const migrateHelperLocations = functions.https.onRequest(async (req, res) => {
    try {
        console.log('Iniciando migração de geohashes para helpers...');

        // Buscar todos os usuários (helpers e não-helpers)
        const usersSnapshot = await admin.firestore()
            .collection('users')
            .get();

        if (usersSnapshot.empty) {
            res.send({ success: true, message: 'Nenhum usuário encontrado', updated: 0 });
            return;
        }

        const batch = admin.firestore().batch();
        let updated = 0;

        usersSnapshot.docs.forEach((doc) => {
            const data = doc.data();

            // Apenas atualizar se tiver coordenadas
            if (data.latitude && data.longitude) {
                const hash = geofireCommon.geohashForLocation([data.latitude, data.longitude]);
                batch.update(doc.ref, {
                    geohash: hash,
                    helperRadius: data.helperRadius || 5000 // padrão 5km
                });
                updated++;
                console.log(`Usuário ${doc.id}: geohash = ${hash}`);
            }
        });

        await batch.commit();
        console.log(`Migração concluída: ${updated} usuários atualizados`);

        res.send({
            success: true,
            message: 'Migração concluída com sucesso',
            updated: updated,
            total: usersSnapshot.size
        });
    } catch (error) {
        console.error('Erro na migração:', error);
        res.status(500).send({ success: false, error: String(error) });
    }
});
