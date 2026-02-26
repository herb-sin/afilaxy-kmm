/**
 * Cloud Function para criar sessão de checkout do Stripe
 * Chamada pelo portal web de profissionais
 */
export const createCheckoutSession = functions.https.onCall(async (data, context) => {
    const stripe = require('stripe')(functions.config().stripe?.secret_key);
    
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
