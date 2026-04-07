import { useNavigate } from "react-router";
import { CheckCircle, Calendar, MapPin, Phone, Home } from "lucide-react";
import { Card } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { motion } from "motion/react";

export default function BookingSuccessScreen() {
  const navigate = useNavigate();

  const bookingReference = "SS-" + Math.random().toString(36).substr(2, 9).toUpperCase();

  const handleAddToCalendar = () => {
    // Integração com Google Calendar (simulado)
    alert("Funcionalidade de adicionar ao calendário seria implementada aqui");
  };

  return (
    <div className="min-h-screen w-full bg-gray-50 flex flex-col">
      <div className="flex-1 flex flex-col items-center justify-center px-6 py-12">
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ duration: 0.5, type: "spring" }}
          className="mb-8"
        >
          <div className="relative">
            <div className="absolute inset-0 bg-[#D4AF37]/20 rounded-full blur-3xl"></div>
            <div className="relative w-28 h-28 bg-gradient-to-br from-[#D4AF37] to-[#B8982E] rounded-full flex items-center justify-center">
              <CheckCircle className="w-16 h-16 text-white" />
            </div>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="text-center mb-8"
        >
          <h1 className="text-3xl font-bold text-[#0A1929] mb-3">
            Marcação Confirmada!
          </h1>
          <p className="text-gray-600 mb-6">
            A sua marcação foi criada com sucesso
          </p>

          <Card className="p-4 border-none shadow-md bg-[#D4AF37]/5 inline-block">
            <p className="text-sm text-gray-600 mb-1">Referência</p>
            <p className="text-xl font-bold text-[#0A1929] font-mono">
              {bookingReference}
            </p>
          </Card>
        </motion.div>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.5 }}
          className="w-full space-y-4"
        >
          <Card className="p-6 border-none shadow-lg">
            <h3 className="font-semibold text-[#0A1929] mb-4">
              Resumo da Marcação
            </h3>
            <div className="space-y-3">
              <div className="flex items-start gap-3 text-gray-600">
                <Calendar className="w-5 h-5 text-[#D4AF37] flex-shrink-0 mt-0.5" />
                <div>
                  <p className="font-semibold text-[#0A1929]">
                    25 de Março, 2026
                  </p>
                  <p className="text-sm">14:30 - Lavagem Premium</p>
                </div>
              </div>
              <div className="flex items-start gap-3 text-gray-600">
                <MapPin className="w-5 h-5 text-[#D4AF37] flex-shrink-0 mt-0.5" />
                <div>
                  <p className="font-semibold text-[#0A1929]">
                    Suds & Shine Solutions
                  </p>
                  <p className="text-sm">
                    Shopping Norte Sul, Piso -1, Leiria
                  </p>
                </div>
              </div>
              <div className="flex items-start gap-3 text-gray-600">
                <Phone className="w-5 h-5 text-[#D4AF37] flex-shrink-0 mt-0.5" />
                <div>
                  <p className="font-semibold text-[#0A1929]">913 005 855</p>
                  <p className="text-sm">Entre em contacto se necessário</p>
                </div>
              </div>
            </div>
          </Card>

          <Card className="p-5 border-none shadow-md bg-blue-50">
            <h4 className="font-semibold text-blue-900 mb-2">
              📧 Confirmação enviada
            </h4>
            <p className="text-sm text-blue-800">
              Enviámos um email de confirmação com todos os detalhes da sua
              marcação.
            </p>
          </Card>
        </motion.div>
      </div>

      <div className="px-6 pb-8 space-y-3">
        <Button
          onClick={handleAddToCalendar}
          variant="outline"
          className="w-full h-14 border-[#0A1929] text-[#0A1929] hover:bg-[#0A1929] hover:text-white rounded-xl"
        >
          <Calendar className="w-5 h-5 mr-2" />
          Adicionar ao Google Calendar
        </Button>

        <Button
          onClick={() => navigate("/bookings")}
          variant="outline"
          className="w-full h-14 border-[#0A1929] text-[#0A1929] hover:bg-[#0A1929] hover:text-white rounded-xl"
        >
          Ver Detalhes da Marcação
        </Button>

        <Button
          onClick={() => navigate("/home")}
          className="w-full h-14 bg-[#D4AF37] hover:bg-[#B8982E] text-[#0A1929] rounded-xl text-lg font-semibold"
        >
          <Home className="w-5 h-5 mr-2" />
          Voltar ao Início
        </Button>
      </div>
    </div>
  );
}
