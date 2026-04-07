import { useNavigate } from "react-router";
import {
  ArrowLeft,
  MapPin,
  Phone,
  Mail,
  Clock,
  MessageCircle,
  HelpCircle,
  ExternalLink,
} from "lucide-react";
import { Card } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Separator } from "../components/ui/separator";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "../components/ui/accordion";

const faqs = [
  {
    question: "Como posso marcar uma lavagem?",
    answer:
      "Pode marcar através da app na secção 'Marcar', escolhendo o serviço, tipo de veículo, data e hora desejados. Também pode ligar para 913 005 855.",
  },
  {
    question: "Quanto tempo demora cada serviço?",
    answer:
      "Lavagem Exterior: 20 min, Lavagem Standard: 30 min, Limpeza Interior: 25 min, Lavagem Premium: 45 min.",
  },
  {
    question: "Como funciona o programa de fidelização?",
    answer:
      "A cada lavagem completa, recebe 1 selo. Quando completar 10 selos, ganha 1 lavagem grátis automaticamente.",
  },
  {
    question: "Posso cancelar ou remarcar?",
    answer:
      "Sim, pode cancelar ou remarcar até 2 horas antes da marcação através da app ou contactando-nos diretamente.",
  },
  {
    question: "Aceitam pagamento com cartão?",
    answer:
      "Sim, aceitamos pagamento em dinheiro, cartão de débito e crédito, e MB Way.",
  },
  {
    question: "Onde estão localizados?",
    answer:
      "Estamos localizados no Shopping Norte Sul, Piso -1, em Leiria. Temos estacionamento gratuito e fácil acesso.",
  },
];

export default function ContactScreen() {
  const navigate = useNavigate();

  const openMaps = () => {
    window.open(
      "https://www.google.com/maps/search/?api=1&query=Shopping+Norte+Sul+Leiria",
      "_blank"
    );
  };

  const openWhatsApp = () => {
    window.open("https://wa.me/351913005855", "_blank");
  };

  return (
    <div className="min-h-screen w-full bg-gray-50 pb-8">
      {/* Header */}
      <div className="bg-gradient-to-b from-[#0A1929] to-[#152C42] rounded-b-3xl pb-8 px-6 pt-12">
        <button
          onClick={() => navigate("/home")}
          className="flex items-center gap-2 text-[#D4AF37] mb-6"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Voltar</span>
        </button>
        <h1 className="text-3xl font-bold text-white mb-2">
          Contacto e Apoio
        </h1>
        <p className="text-gray-400">Estamos aqui para ajudar</p>
      </div>

      <div className="px-6 -mt-4 space-y-4">
        {/* Contactos Rápidos */}
        <Card className="p-6 border-none shadow-lg">
          <h3 className="font-semibold text-[#0A1929] mb-4">
            Informações de Contacto
          </h3>
          <div className="space-y-4">
            <a
              href="tel:913005855"
              className="flex items-center gap-3 text-gray-600 hover:text-[#D4AF37] transition-colors"
            >
              <div className="w-10 h-10 bg-[#D4AF37]/10 rounded-xl flex items-center justify-center flex-shrink-0">
                <Phone className="w-5 h-5 text-[#D4AF37]" />
              </div>
              <div>
                <p className="font-semibold text-[#0A1929] text-sm">Telefone</p>
                <p className="text-sm">913 005 855</p>
              </div>
            </a>

            <a
              href="mailto:info@sudsshine.pt"
              className="flex items-center gap-3 text-gray-600 hover:text-[#D4AF37] transition-colors"
            >
              <div className="w-10 h-10 bg-[#D4AF37]/10 rounded-xl flex items-center justify-center flex-shrink-0">
                <Mail className="w-5 h-5 text-[#D4AF37]" />
              </div>
              <div>
                <p className="font-semibold text-[#0A1929] text-sm">Email</p>
                <p className="text-sm">info@sudsshine.pt</p>
              </div>
            </a>

            <button
              onClick={openMaps}
              className="w-full flex items-center gap-3 text-gray-600 hover:text-[#D4AF37] transition-colors"
            >
              <div className="w-10 h-10 bg-[#D4AF37]/10 rounded-xl flex items-center justify-center flex-shrink-0">
                <MapPin className="w-5 h-5 text-[#D4AF37]" />
              </div>
              <div className="text-left flex-1">
                <p className="font-semibold text-[#0A1929] text-sm">
                  Morada
                </p>
                <p className="text-sm">
                  Shopping Norte Sul, Piso -1
                  <br />
                  Leiria, Portugal
                </p>
              </div>
              <ExternalLink className="w-4 h-4 text-gray-400" />
            </button>
          </div>
        </Card>

        {/* Horário */}
        <Card className="p-6 border-none shadow-lg">
          <div className="flex items-center gap-2 mb-4">
            <Clock className="w-5 h-5 text-[#D4AF37]" />
            <h3 className="font-semibold text-[#0A1929]">
              Horário de Funcionamento
            </h3>
          </div>
          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Segunda a Sexta</span>
              <span className="font-semibold text-[#0A1929]">09:00 - 19:00</span>
            </div>
            <Separator />
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Sábado</span>
              <span className="font-semibold text-[#0A1929]">09:00 - 13:00</span>
            </div>
            <Separator />
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Domingo</span>
              <span className="font-semibold text-red-600">Encerrado</span>
            </div>
          </div>
        </Card>

        {/* Ações Rápidas */}
        <div className="grid grid-cols-2 gap-3">
          <Button
            onClick={openWhatsApp}
            className="h-24 bg-green-600 hover:bg-green-700 text-white rounded-xl flex flex-col items-center justify-center gap-2"
          >
            <MessageCircle className="w-6 h-6" />
            <span className="text-sm">WhatsApp</span>
          </Button>
          <Button
            onClick={() => navigate("/booking/service")}
            className="h-24 bg-[#D4AF37] hover:bg-[#B8982E] text-[#0A1929] rounded-xl flex flex-col items-center justify-center gap-2"
          >
            <Phone className="w-6 h-6" />
            <span className="text-sm">Ligar Agora</span>
          </Button>
        </div>

        {/* FAQ */}
        <Card className="p-6 border-none shadow-lg">
          <div className="flex items-center gap-2 mb-4">
            <HelpCircle className="w-5 h-5 text-[#D4AF37]" />
            <h3 className="font-semibold text-[#0A1929]">
              Perguntas Frequentes
            </h3>
          </div>
          <Accordion type="single" collapsible className="w-full">
            {faqs.map((faq, index) => (
              <AccordionItem key={index} value={`item-${index}`}>
                <AccordionTrigger className="text-left text-sm font-semibold text-[#0A1929] hover:text-[#D4AF37]">
                  {faq.question}
                </AccordionTrigger>
                <AccordionContent className="text-sm text-gray-600">
                  {faq.answer}
                </AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        </Card>

        {/* Mapa (Placeholder) */}
        <Card className="p-6 border-none shadow-lg">
          <h3 className="font-semibold text-[#0A1929] mb-4">Como Chegar</h3>
          <button
            onClick={openMaps}
            className="w-full h-48 bg-gray-200 rounded-xl flex items-center justify-center hover:bg-gray-300 transition-colors"
          >
            <div className="text-center">
              <MapPin className="w-12 h-12 text-[#D4AF37] mx-auto mb-2" />
              <p className="font-semibold text-[#0A1929]">
                Ver no Google Maps
              </p>
              <p className="text-sm text-gray-600">
                Shopping Norte Sul, Piso -1
              </p>
            </div>
          </button>
        </Card>

        {/* Estatísticas */}
        <Card className="p-6 border-none shadow-lg bg-gradient-to-br from-[#0A1929] to-[#152C42]">
          <h3 className="font-semibold text-white mb-4">Por que escolher-nos</h3>
          <div className="grid grid-cols-3 gap-4">
            <div className="text-center">
              <p className="text-3xl font-bold text-[#D4AF37] mb-1">500+</p>
              <p className="text-xs text-gray-400">Carros Tratados</p>
            </div>
            <div className="text-center">
              <p className="text-3xl font-bold text-[#D4AF37] mb-1">4.9</p>
              <p className="text-xs text-gray-400">Avaliação Média</p>
            </div>
            <div className="text-center">
              <p className="text-3xl font-bold text-[#D4AF37] mb-1">3+</p>
              <p className="text-xs text-gray-400">Anos Experiência</p>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}