import { useNavigate } from "react-router";
import {
  Sparkles,
  Calendar,
  Clock,
  User,
  Phone,
  Mail,
  MapPin,
  Car,
  Euro,
  ArrowLeft,
  Edit,
} from "lucide-react";
import { Card } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Separator } from "../components/ui/separator";

export default function BookingConfirmationScreen() {
  const navigate = useNavigate();

  // Dados de exemplo (seriam passados via state/context)
  const bookingData = {
    service: "Lavagem Premium",
    vehicleType: "Passageiros",
    date: "25 de Março, 2026",
    time: "14:30",
    name: "João Silva",
    phone: "913 005 855",
    email: "joao.silva@exemplo.com",
    notes: "Por favor, dar atenção especial aos estofos.",
    price: 32.0,
  };

  const handleConfirm = () => {
    navigate("/booking/success");
  };

  return (
    <div className="min-h-screen w-full bg-gray-50 pb-32">
      {/* Header */}
      <div className="bg-gradient-to-b from-[#0A1929] to-[#152C42] rounded-b-3xl pb-8 px-6 pt-12">
        <button
          onClick={() => navigate("/booking/contact")}
          className="flex items-center gap-2 text-[#D4AF37] mb-6"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Voltar</span>
        </button>
        <h1 className="text-3xl font-bold text-white mb-2">
          Confirmar Marcação
        </h1>
        <p className="text-gray-400">Reveja os detalhes antes de confirmar</p>
      </div>

      <div className="px-6 -mt-4 space-y-4">
        {/* Serviço */}
        <Card className="p-6 border-none shadow-lg">
          <div className="flex items-start justify-between mb-4">
            <h3 className="font-semibold text-[#0A1929]">Detalhes do Serviço</h3>
            <button
              onClick={() => navigate("/booking/service")}
              className="text-[#D4AF37] text-sm flex items-center gap-1"
            >
              <Edit className="w-4 h-4" />
              Editar
            </button>
          </div>
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-[#D4AF37]/10 rounded-lg flex items-center justify-center">
                <Sparkles className="w-5 h-5 text-[#D4AF37]" />
              </div>
              <div>
                <p className="font-semibold text-[#0A1929]">
                  {bookingData.service}
                </p>
                <p className="text-sm text-gray-600">
                  Veículo: {bookingData.vehicleType}
                </p>
              </div>
            </div>
          </div>
        </Card>

        {/* Data e Hora */}
        <Card className="p-6 border-none shadow-lg">
          <div className="flex items-start justify-between mb-4">
            <h3 className="font-semibold text-[#0A1929]">Data e Hora</h3>
            <button
              onClick={() => navigate("/booking/datetime")}
              className="text-[#D4AF37] text-sm flex items-center gap-1"
            >
              <Edit className="w-4 h-4" />
              Editar
            </button>
          </div>
          <div className="space-y-3">
            <div className="flex items-center gap-3 text-gray-600">
              <Calendar className="w-5 h-5 text-[#D4AF37]" />
              <span>{bookingData.date}</span>
            </div>
            <div className="flex items-center gap-3 text-gray-600">
              <Clock className="w-5 h-5 text-[#D4AF37]" />
              <span>{bookingData.time}</span>
            </div>
          </div>
        </Card>

        {/* Dados de Contacto */}
        <Card className="p-6 border-none shadow-lg">
          <div className="flex items-start justify-between mb-4">
            <h3 className="font-semibold text-[#0A1929]">Seus Dados</h3>
            <button
              onClick={() => navigate("/booking/contact")}
              className="text-[#D4AF37] text-sm flex items-center gap-1"
            >
              <Edit className="w-4 h-4" />
              Editar
            </button>
          </div>
          <div className="space-y-3">
            <div className="flex items-center gap-3 text-gray-600">
              <User className="w-5 h-5 text-[#D4AF37]" />
              <span>{bookingData.name}</span>
            </div>
            <div className="flex items-center gap-3 text-gray-600">
              <Phone className="w-5 h-5 text-[#D4AF37]" />
              <span>{bookingData.phone}</span>
            </div>
            <div className="flex items-center gap-3 text-gray-600">
              <Mail className="w-5 h-5 text-[#D4AF37]" />
              <span>{bookingData.email}</span>
            </div>
            {bookingData.notes && (
              <>
                <Separator className="my-2" />
                <p className="text-sm text-gray-600 italic">
                  "{bookingData.notes}"
                </p>
              </>
            )}
          </div>
        </Card>

        {/* Localização */}
        <Card className="p-6 border-none shadow-lg">
          <h3 className="font-semibold text-[#0A1929] mb-4">Localização</h3>
          <div className="flex items-start gap-3 text-gray-600">
            <MapPin className="w-5 h-5 text-[#D4AF37] flex-shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold text-[#0A1929]">
                Suds & Shine Solutions
              </p>
              <p className="text-sm">Shopping Norte Sul, Piso -1</p>
              <p className="text-sm">Leiria, Portugal</p>
            </div>
          </div>
        </Card>

        {/* Resumo de Preço */}
        <Card className="p-6 border-none shadow-lg bg-gradient-to-br from-[#0A1929] to-[#152C42]">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold text-white">Total a Pagar</h3>
            <Euro className="w-6 h-6 text-[#D4AF37]" />
          </div>
          <div className="space-y-2">
            <div className="flex justify-between text-gray-300">
              <span>{bookingData.service}</span>
              <span>{bookingData.price.toFixed(2).replace(".", ",")}€</span>
            </div>
            <Separator className="bg-white/20" />
            <div className="flex justify-between items-center">
              <span className="text-white text-lg font-semibold">Total</span>
              <span className="text-[#D4AF37] text-2xl font-bold">
                {bookingData.price.toFixed(2).replace(".", ",")}€
              </span>
            </div>
          </div>
        </Card>
      </div>

      {/* Fixed Bottom Button */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-6">
        <Button
          onClick={handleConfirm}
          className="w-full h-14 bg-[#D4AF37] hover:bg-[#B8982E] text-[#0A1929] rounded-xl text-lg font-semibold"
        >
          Confirmar Marcação
        </Button>
      </div>
    </div>
  );
}
