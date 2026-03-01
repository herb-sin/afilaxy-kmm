package com.afilaxy.data.repository

import com.afilaxy.domain.model.ContentCategory
import com.afilaxy.domain.model.EducationalContent
import com.afilaxy.domain.model.getCurrentTimeMillis
import com.afilaxy.domain.repository.EducationalContentRepository

class EducationalContentRepositoryImpl : EducationalContentRepository {
    
    private val contents = listOf(
        EducationalContent(
            id = "1",
            title = "Bombinha Azul: Medicação de Resgate",
            category = ContentCategory.RESCUE,
            summary = "Entenda quando e como usar a medicação de alívio rápido",
            content = """
                # Medicação de Resgate (Bombinha Azul)
                
                ## O que é?
                A bombinha azul (broncodilatador) é uma medicação de **alívio rápido** usada durante crises de asma.
                
                ## Quando usar?
                - Durante falta de ar
                - Chiado no peito
                - Tosse persistente
                - Aperto no peito
                
                ## Como usar?
                1. Agite bem o inalador
                2. Expire completamente
                3. Coloque o bocal na boca
                4. Inspire profundamente enquanto pressiona
                5. Segure a respiração por 10 segundos
                
                ## ⚠️ IMPORTANTE
                - Não substitui o tratamento preventivo
                - Usar mais de 2x por semana indica asma descontrolada
                - Procure um médico se precisar usar frequentemente
                
                ## Efeitos colaterais comuns
                - Tremor nas mãos
                - Batimento cardíaco acelerado
                - Nervosismo
                
                Esses efeitos são temporários e normais.
            """.trimIndent(),
            author = "Dr. João Silva - Pneumologista",
            readTimeMinutes = 5,
            createdAt = getCurrentTimeMillis()
        ),
        EducationalContent(
            id = "2",
            title = "Bombinha Laranja: Medicação de Manutenção",
            category = ContentCategory.MAINTENANCE,
            summary = "Por que o tratamento preventivo é essencial",
            content = """
                # Medicação de Manutenção (Bombinha Laranja)
                
                ## O que é?
                A bombinha laranja (corticoide inalado) é uma medicação **preventiva** que controla a inflamação dos pulmões.
                
                ## Por que usar TODO DIA?
                - Reduz inflamação crônica
                - Previne crises
                - Melhora qualidade de vida
                - Evita idas à emergência
                
                ## Como funciona?
                Diferente da azul, a laranja:
                - NÃO alivia sintomas imediatamente
                - Age lentamente reduzindo inflamação
                - Precisa ser usada TODOS OS DIAS
                - Efeito aparece após 2-4 semanas
                
                ## Horários recomendados
                - Manhã (ao acordar)
                - Noite (antes de dormir)
                
                ## ⚠️ NUNCA PARE POR CONTA PRÓPRIA
                Mesmo sem sintomas, continue usando. A asma é uma doença crônica que precisa de controle contínuo.
                
                ## Mitos e Verdades
                ❌ "Vicia" - FALSO
                ❌ "Engorda" - FALSO (dose inalada é mínima)
                ✅ Previne crises - VERDADEIRO
                ✅ É seguro para uso prolongado - VERDADEIRO
            """.trimIndent(),
            author = "Dra. Maria Santos - Pneumologista",
            readTimeMinutes = 7,
            createdAt = getCurrentTimeMillis()
        ),
        EducationalContent(
            id = "3",
            title = "Como Conseguir Medicação Gratuita no SUS",
            category = ContentCategory.SUS,
            summary = "Passo a passo para obter tratamento gratuito",
            content = """
                # Medicação Gratuita no SUS
                
                ## Programa Farmácia Popular
                O SUS oferece medicação para asma **100% GRATUITA**.
                
                ## Documentos necessários
                - RG e CPF
                - Receita médica (válida por 120 dias)
                - Cartão SUS
                
                ## Onde retirar?
                1. **UBS (Unidade Básica de Saúde)**
                   - Procure a UBS mais próxima
                   - Leve os documentos
                   - Retire mensalmente
                
                2. **Farmácia Popular**
                   - Rede de farmácias conveniadas
                   - Mais de 30 mil pontos no Brasil
                
                ## Medicações disponíveis
                ✅ Brometo de Ipratrópio
                ✅ Budesonida
                ✅ Fenoterol
                ✅ Salbutamol
                
                ## Dicas importantes
                - Não precisa comprovar renda
                - Receita pode ser de médico particular
                - Válido em todo território nacional
                - Leve sempre a receita original
                
                ## Encontre a UBS mais próxima
                Use o app Afilaxy para localizar postos de saúde na sua região!
            """.trimIndent(),
            author = "Equipe Afilaxy",
            readTimeMinutes = 6,
            createdAt = getCurrentTimeMillis()
        ),
        EducationalContent(
            id = "4",
            title = "Perguntas Frequentes sobre Asma",
            category = ContentCategory.FAQ,
            summary = "Tire suas dúvidas sobre asma e tratamento",
            content = """
                # Perguntas Frequentes
                
                ## 1. Asma tem cura?
                Não tem cura, mas tem **controle total**. Com tratamento adequado, você pode viver normalmente.
                
                ## 2. Posso fazer exercícios?
                **SIM!** Exercícios são recomendados. Use a bombinha azul 15 minutos antes se necessário.
                
                ## 3. Asma é psicológica?
                **NÃO.** É uma doença inflamatória real dos pulmões. Estresse pode piorar, mas não é a causa.
                
                ## 4. Crianças "superam" a asma?
                Algumas melhoram na adolescência, mas a asma pode retornar na vida adulta. Acompanhamento é essencial.
                
                ## 5. Bombinha vicia?
                **NÃO.** Isso é um mito perigoso. A medicação é segura e necessária.
                
                ## 6. Posso parar quando melhorar?
                **NÃO.** Você melhorou PORQUE está usando. Parar = voltar a ter crises.
                
                ## 7. Qual a diferença entre asma e bronquite?
                Asma é crônica e reversível. Bronquite pode ser aguda (infecção) ou crônica (tabagismo).
                
                ## 8. Animais de estimação pioram?
                Depende. Se você tem alergia a pelos, sim. Mas nem todo asmático é alérgico.
                
                ## 9. Mudança de clima afeta?
                Sim. Ar frio e seco pode irritar as vias aéreas. Use lenço no nariz/boca no frio.
                
                ## 10. Quando procurar emergência?
                - Falta de ar intensa
                - Lábios/unhas roxos
                - Bombinha azul não alivia
                - Dificuldade para falar
                
                **LIGUE 192 (SAMU) IMEDIATAMENTE**
            """.trimIndent(),
            author = "Dr. Carlos Mendes - Alergista",
            readTimeMinutes = 8,
            createdAt = getCurrentTimeMillis()
        )
    )
    
    override suspend fun getAll(): List<EducationalContent> {
        return contents
    }
    
    override suspend fun getById(id: String): EducationalContent? {
        return contents.find { it.id == id }
    }
    
    override suspend fun getByCategory(category: ContentCategory): List<EducationalContent> {
        return contents.filter { it.category == category }
    }
}
