const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendEmergencyNotification = onDocumentCreated(
    {
      document: "push_notifications/{notificationId}",
      region: "us-central1",
    },
    async (event) => {
      console.log("🚨 sendEmergencyNotification iniciada");

      const data = event.data.data();
      console.log("📋 Dados:", JSON.stringify(data, null, 2));

      try {
        const userDoc = await admin.firestore()
            .collection("users")
            .doc(data.to)
            .get();

        if (!userDoc.exists) {
          console.log("❌ Usuário não encontrado:", data.to);
          return null;
        }

        const fcmToken = userDoc.data().fcmToken;
        if (!fcmToken) {
          console.log("⚠️ Token FCM não encontrado");
          return null;
        }

        const message = {
          token: fcmToken,
          data: {
            type: data.data.type,
            emergencyId: data.data.emergencyId,
            requesterName: data.data.requesterName,
            title: data.data.title,
            body: data.data.body,
          },
          android: {
            priority: "high",
            notification: {
              channelId: "afilaxy_emergency",
            },
          },
        };

        const response = await admin.messaging().send(message);
        console.log("✅ Notificação enviada:", response);

        await event.data.ref.update({
          processed: true,
          sentAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      } catch (error) {
        console.error("❌ Erro:", error);
        await event.data.ref.update({
          error: error.message,
          processed: false,
        });
      }

      return null;
    },
);
