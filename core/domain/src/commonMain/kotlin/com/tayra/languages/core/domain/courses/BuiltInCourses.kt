package com.tayra.languages.core.domain.courses

/** The courses shipped with the app, until they come from a server. */
class BuiltInCourses : CourseSource {
    override suspend fun courses(languageCode: String): List<Course> = ALL.filter { it.languageCode == languageCode.lowercase() }
    override suspend fun course(id: String): Course? = ALL.firstOrNull { it.id == id }

    companion object {
        private fun lesson(id: String, title: String, summary: String, text: String) = Lesson(id, title, summary, text.trimIndent().trim())

        private val FIRST_STEPS = Course(
            id = "pt-primeiros-passos",
            languageCode = "pt",
            title = "Primeiros passos",
            description = "Short texts in simple Brazilian Portuguese: introductions, family, home and everyday routines.",
            level = CourseLevel.A1,
            topic = "Everyday life",
            lessons = listOf(
                lesson(
                    "pt-primeiros-passos-1", "Olá! Eu sou a Ana", "Ana introduces herself: name, age, city and work.",
                    """
                    Olá! Meu nome é Ana. Eu tenho vinte e oito anos e sou brasileira. Eu moro em São Paulo, uma cidade muito grande.

                    Eu sou professora. Eu trabalho em uma escola perto da minha casa. Eu gosto muito do meu trabalho, porque eu gosto de crianças.

                    Eu falo português e um pouco de inglês. Agora eu estudo espanhol. Eu estudo à noite, depois do trabalho.

                    E você? Qual é o seu nome? Onde você mora?
                    """,
                ),
                lesson(
                    "pt-primeiros-passos-2", "Minha família", "Ana describes her parents, her brother and her grandmother.",
                    """
                    Minha família não é grande. Meu pai se chama Paulo e minha mãe se chama Helena. Eles moram em uma casa pequena no interior.

                    Eu tenho um irmão. O nome dele é Lucas. Ele tem vinte e três anos e estuda medicina. Lucas é alto e muito engraçado.

                    Minha avó mora com os meus pais. Ela tem oitenta anos e cozinha muito bem. O bolo de laranja dela é o melhor do mundo.

                    Nos domingos, a família toda almoça junta. Nós comemos, conversamos e rimos muito.
                    """,
                ),
                lesson(
                    "pt-primeiros-passos-3", "Minha casa", "A small apartment, room by room.",
                    """
                    Eu moro em um apartamento pequeno no centro da cidade. O apartamento tem uma sala, um quarto, uma cozinha e um banheiro.

                    A sala é clara e tem uma janela grande. Na sala há um sofá azul, uma mesa e muitos livros. Eu gosto de ler no sofá.

                    A cozinha é pequena, mas eu cozinho todos os dias. No quarto há uma cama, um armário e uma mesa para estudar.

                    O apartamento não tem jardim, mas eu tenho muitas plantas na janela. Eu gosto da minha casa.
                    """,
                ),
                lesson(
                    "pt-primeiros-passos-4", "Meu dia", "A normal working day, from morning to night.",
                    """
                    Eu acordo às seis horas da manhã. Primeiro, eu tomo banho. Depois, eu tomo café da manhã: pão, queijo e um café com leite.

                    Às sete horas eu saio de casa. Eu vou para a escola de ônibus. As aulas começam às oito e terminam ao meio-dia.

                    Eu almoço em um restaurante perto da escola. À tarde, eu preparo as aulas e corrijo os trabalhos dos alunos.

                    À noite, eu volto para casa, janto e estudo espanhol. Eu durmo às onze horas. Meu dia é longo, mas é bom.
                    """,
                ),
                lesson(
                    "pt-primeiros-passos-5", "No café", "Ordering a coffee and something to eat.",
                    """
                    Hoje é sábado. Ana encontra a amiga Carla em um café.

                    — Bom dia! O que vocês querem? — pergunta o garçom.

                    — Eu quero um café com leite e um pão de queijo, por favor — responde Ana.

                    — Para mim, um suco de laranja e um pedaço de bolo — diz Carla.

                    O garçom traz os pedidos. O café está quente e o bolo está muito gostoso. As duas amigas conversam por uma hora.

                    — A conta, por favor — pede Ana.

                    — São vinte e cinco reais — responde o garçom.

                    Ana paga a conta e as amigas vão embora felizes.
                    """,
                ),
            ),
        )

        private val CITY_LIFE = Course(
            id = "pt-vida-na-cidade",
            languageCode = "pt",
            title = "A vida na cidade",
            description = "Getting things done in a Brazilian city: shopping, transport, eating out, the doctor and a weekend away.",
            level = CourseLevel.A2,
            topic = "City and travel",
            lessons = listOf(
                lesson(
                    "pt-vida-na-cidade-1", "No supermercado", "A weekly shop, with prices and a forgotten list.",
                    """
                    Toda sexta-feira, depois do trabalho, eu passo no supermercado. Normalmente eu levo uma lista, mas ontem eu esqueci a lista em casa.

                    Primeiro fui até a seção de frutas e verduras. Comprei bananas, maçãs, tomates e uma alface bem fresca. As laranjas estavam caras, então não levei.

                    Depois peguei arroz, feijão, macarrão e um pacote de café. Na padaria do supermercado, pedi seis pães e um pedaço de queijo.

                    No caixa, a fila estava enorme. Esperei quase vinte minutos. Quando cheguei em casa, percebi que tinha esquecido o leite. Sempre esqueço alguma coisa!
                    """,
                ),
                lesson(
                    "pt-vida-na-cidade-2", "Pegando o ônibus", "Asking the way and taking the right bus.",
                    """
                    Marcos é novo na cidade e precisa ir ao centro. Ele não conhece as linhas de ônibus, então pergunta a uma senhora no ponto.

                    — Com licença, a senhora sabe qual ônibus vai para o centro?

                    — Sei, sim. Você pode pegar o quarenta e dois ou o cinquenta. O quarenta e dois é mais rápido.

                    — E quanto tempo demora?

                    — Uns vinte minutos, se não tiver trânsito. Você desce na praça da Sé, é o último ponto.

                    — Muito obrigado!

                    O ônibus chega cheio, mas Marcos consegue entrar. Ele paga a passagem com o cartão e fica em pé perto da janela. Pela primeira vez, ele vê as ruas da nova cidade.
                    """,
                ),
                lesson(
                    "pt-vida-na-cidade-3", "No restaurante", "Choosing from the menu and a small mistake with the bill.",
                    """
                    No sábado à noite, Júlia e Pedro foram jantar em um restaurante novo do bairro. O lugar estava cheio, mas eles tinham feito uma reserva.

                    O garçom trouxe o cardápio e explicou o prato do dia: peixe grelhado com arroz e legumes. Júlia pediu o peixe. Pedro preferiu um bife com batatas fritas e uma salada.

                    A comida demorou um pouco, mas estava deliciosa. De sobremesa, eles dividiram um pudim de leite.

                    Quando a conta chegou, Pedro percebeu um erro: o garçom tinha cobrado duas sobremesas. O garçom pediu desculpas e corrigiu o valor. Eles deixaram uma boa gorjeta e prometeram voltar.
                    """,
                ),
                lesson(
                    "pt-vida-na-cidade-4", "Uma consulta médica", "Describing symptoms and understanding the doctor's advice.",
                    """
                    Há três dias Rafael não se sente bem. Ele tem dor de cabeça, dor de garganta e um pouco de febre. Hoje ele decidiu ir ao médico.

                    — Bom dia, Rafael. O que você está sentindo? — perguntou a médica.

                    — Estou com muita dor de garganta e me sinto cansado o tempo todo.

                    A médica examinou a garganta dele e mediu a temperatura.

                    — Não é nada grave, é só uma gripe. Você precisa descansar, beber bastante água e tomar este remédio duas vezes por dia.

                    — Posso trabalhar amanhã?

                    — É melhor ficar em casa por dois dias. Se a febre não passar, volte aqui.

                    Rafael agradeceu, passou na farmácia e foi direto para a cama.
                    """,
                ),
                lesson(
                    "pt-vida-na-cidade-5", "Um fim de semana no Rio", "A short trip: the beach, the views and the food.",
                    """
                    No mês passado, eu e minha irmã passamos um fim de semana no Rio de Janeiro. Viajamos de ônibus na sexta à noite e chegamos no sábado bem cedo.

                    Deixamos as malas no hotel e fomos direto para a praia de Copacabana. O mar estava calmo e o sol estava forte. Tomamos água de coco e caminhamos pela areia.

                    À tarde, subimos até o Cristo Redentor. A vista lá de cima é incrível: dá para ver a cidade inteira, as montanhas e o mar.

                    No domingo, visitamos o Jardim Botânico e almoçamos em um restaurante simples, com feijão preto e peixe frito. Voltamos para casa cansadas, mas muito felizes. Quero voltar em breve.
                    """,
                ),
            ),
        )

        private val SHORT_STORIES = Course(
            id = "pt-historias-curtas",
            languageCode = "pt",
            title = "Histórias curtas",
            description = "Short stories with a twist, in richer language: past tenses, feelings and a little suspense.",
            level = CourseLevel.B1,
            topic = "Stories",
            lessons = listOf(
                lesson(
                    "pt-historias-curtas-1", "A carta sem remetente", "An unsigned letter arrives after many years.",
                    """
                    Numa manhã de chuva, dona Teresa encontrou uma carta debaixo da porta. O envelope era amarelo, antigo, e não tinha nome de remetente.

                    Ela abriu a carta com cuidado. A letra era pequena e elegante. "Querida Teresa", dizia, "faz cinquenta anos que eu queria escrever para você. Nunca tive coragem."

                    Teresa sentou-se na cadeira da cozinha. Enquanto lia, lembrou-se de um verão distante, de uma praça e de um rapaz que tocava violão.

                    A carta terminava assim: "Estarei na mesma praça no domingo, às quatro. Se você não vier, eu vou entender."

                    Teresa olhou o calendário. Era sábado. Ela sorriu, guardou a carta no bolso e foi escolher um vestido.
                    """,
                ),
                lesson(
                    "pt-historias-curtas-2", "O guarda-chuva trocado", "Two strangers take each other's umbrella by mistake.",
                    """
                    Chovia muito quando Bruno saiu da biblioteca. Com pressa, pegou um guarda-chuva preto na entrada e correu para o ponto de ônibus.

                    Só em casa percebeu o engano: aquele guarda-chuva não era o dele. No cabo, havia uma etiqueta com um nome e um número de telefone: "Clara".

                    Ele ligou no dia seguinte, um pouco envergonhado. Do outro lado, uma voz riu.

                    — Então foi você! Eu fiquei com o seu. O seu é horrível, sabia? Está todo quebrado.

                    Combinaram de trocar os guarda-chuvas num café. Conversaram durante três horas e esqueceram completamente o motivo do encontro.

                    Quando se despediram, cada um levou outra vez o guarda-chuva errado. Desta vez, de propósito.
                    """,
                ),
                lesson(
                    "pt-historias-curtas-3", "A última viagem do trem", "A railway worker's last day on an old line.",
                    """
                    Seu Antônio trabalhou quarenta anos na pequena estação de Santa Rita. Conhecia cada passageiro pelo nome e sabia o horário de todos os trens de cor.

                    Um dia chegou a notícia: a linha seria fechada. Quase ninguém viajava mais de trem, e a empresa não queria gastar dinheiro com a manutenção.

                    Na manhã da última viagem, a plataforma estava cheia. Vieram antigos passageiros, crianças que nunca tinham visto um trem e até o prefeito da cidade.

                    Seu Antônio vestiu o uniforme, que já estava apertado, e apitou pela última vez. O trem partiu devagar, soltando fumaça.

                    Quando o silêncio voltou, ele fechou a porta da estação, mas não devolveu a chave. Algumas coisas, pensou, a gente não entrega.
                    """,
                ),
                lesson(
                    "pt-historias-curtas-4", "O vizinho do terceiro andar", "Everyone has a theory about the quiet neighbour.",
                    """
                    Ninguém no prédio sabia nada sobre o vizinho do terceiro andar. Ele saía sempre à noite, com uma mala pesada, e só voltava de madrugada.

                    Dona Lúcia, do segundo andar, tinha certeza de que ele era um ladrão. O porteiro achava que era um músico. As crianças preferiam acreditar que era um espião.

                    Certa noite, faltou luz no bairro inteiro. No escuro, ouviu-se uma música suave vindo do terceiro andar. Os vizinhos subiram as escadas, curiosos, com velas nas mãos.

                    A porta estava aberta. Lá dentro, o homem misterioso consertava relógios antigos, dezenas deles, à luz de uma lanterna. Um deles tocava a música.

                    — Entrem — disse ele, sem levantar os olhos. — Eu estava mesmo precisando de companhia.
                    """,
                ),
            ),
        )

        val ALL: List<Course> = listOf(FIRST_STEPS, CITY_LIFE, SHORT_STORIES)
    }
}
