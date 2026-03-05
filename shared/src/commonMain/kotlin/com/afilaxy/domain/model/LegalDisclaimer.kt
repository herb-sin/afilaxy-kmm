package com.afilaxy.domain.model

/**
 * Disclaimer legal e de privacidade
 * Mitigação de riscos LGPD e responsabilidade civil
 */
object LegalDisclaimer {
    
    const val EMERGENCY_WARNING = """
        ⚠️ ATENÇÃO: Este aplicativo NÃO substitui atendimento médico de emergência.
        
        Em caso de crise grave de asma, ligue imediatamente:
        • SAMU: 192
        • Bombeiros: 193
        
        O Afilaxy conecta pessoas para compartilhamento de medicação de resgate em situações de urgência, mas não substitui profissionais de saúde.
    """
    
    const val PRIVACY_NOTICE = """
        🔒 PRIVACIDADE E DADOS
        
        Sua localização é coletada APENAS durante emergências ativas e compartilhada apenas com helpers próximos que aceitaram ajudar.
        
        • Dados são criptografados
        • Localização não é rastreada continuamente
        • Você pode desativar o modo helper a qualquer momento
        • Histórico pode ser apagado
        
        Conforme LGPD (Lei 13.709/2018), você tem direito a:
        • Acessar seus dados
        • Corrigir dados incorretos
        • Solicitar exclusão
        • Revogar consentimento
    """
    
    const val MEDICATION_DISCLAIMER = """
        💊 SOBRE MEDICAÇÃO
        
        Este app facilita o acesso a medicação de resgate (broncodilatadores) em emergências, mas:
        
        • Não prescrevemos medicamentos
        • Não substituímos consulta médica
        • Recomendamos tratamento preventivo no SUS
        • Incentivamos acompanhamento com pneumologista
    """
}
