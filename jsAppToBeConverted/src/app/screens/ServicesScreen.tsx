import { useNavigate } from "react-router";
import {
  Sparkles,
  Car,
  Droplets,
  Sofa,
  Clock,
  ArrowLeft,
  ChevronRight,
  Shield,
  Wind,
  Circle,
} from "lucide-react";
import { Card } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { BottomNav } from "../components/BottomNav";

const services = [
  {
    id: "standard",
    name: "Lavagem Standard",
    description: "Lavagem completa exterior e interior",
    duration: "30 min",
    prices: { passenger: "25,00€", suv: "27,00€" },
    icon: Car,
    popular: false,
  },
  {
    id: "premium",
    name: "Lavagem Premium",
    description: "Lavagem detalhada com acabamento premium",
    duration: "45 min",
    prices: { passenger: "32,00€", suv: "34,00€" },
    icon: Sparkles,
    popular: true,
  },
  {
    id: "exterior",
    name: "Lavagem Exterior",
    description: "Apenas lavagem exterior",
    duration: "20 min",
    prices: { passenger: "16,00€", suv: "18,50€" },
    icon: Droplets,
    popular: false,
  },
  {
    id: "interior",
    name: "Limpeza do Interior",
    description: "Apenas limpeza interior",
    duration: "25 min",
    prices: { passenger: "16,00€", suv: "18,50€" },
    icon: Sofa,
    popular: false,
  },
];

const extras = [
  { id: "wax", name: "Enceramento", price: "15,00€", icon: Shield },
  { id: "vacuum", name: "Aspiração Profunda", price: "8,00€", icon: Wind },
  { id: "tires", name: "Brilho de Pneus", price: "5,00€", icon: Circle },
  { id: "odor", name: "Tratamento de Odores", price: "12,00€", icon: Wind },
  { id: "upholstery", name: "Limpeza de Estofos", price: "20,00€", icon: Sofa },
];

export default function ServicesScreen() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen w-full bg-gray-50 pb-24">
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
          Nossos Serviços
        </h1>
        <p className="text-gray-400">
          Escolha o serviço perfeito para o seu veículo
        </p>
      </div>

      <div className="px-6 space-y-6 -mt-4">
        {/* Serviços Principais */}
        <div className="space-y-3">
          {services.map((service) => {
            const Icon = service.icon;
            return (
              <Card
                key={service.id}
                onClick={() => navigate("/booking/service")}
                className="p-5 border-none shadow-md cursor-pointer hover:shadow-lg transition-all relative overflow-hidden"
              >
                {service.popular && (
                  <Badge className="absolute top-4 right-4 bg-[#D4AF37] text-[#0A1929] border-none">
                    Mais Popular
                  </Badge>
                )}
                <div className="flex gap-4">
                  <div className="w-16 h-16 bg-[#D4AF37]/10 rounded-2xl flex items-center justify-center flex-shrink-0">
                    <Icon className="w-8 h-8 text-[#D4AF37]" />
                  </div>
                  <div className="flex-1">
                    <h3 className="font-bold text-[#0A1929] mb-1">
                      {service.name}
                    </h3>
                    <p className="text-sm text-gray-600 mb-3">
                      {service.description}
                    </p>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-4">
                        <div>
                          <p className="text-xs text-gray-500">Passageiros</p>
                          <p className="text-lg font-bold text-[#D4AF37]">
                            {service.prices.passenger}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-gray-500">SUV</p>
                          <p className="text-lg font-bold text-[#D4AF37]">
                            {service.prices.suv}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-1 text-gray-600">
                        <Clock className="w-4 h-4" />
                        <span className="text-sm">{service.duration}</span>
                      </div>
                    </div>
                  </div>
                </div>
                <ChevronRight className="absolute right-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              </Card>
            );
          })}
        </div>

        {/* Extras */}
        <div>
          <h2 className="text-xl font-bold text-[#0A1929] mb-4">
            Extras Disponíveis
          </h2>
          <div className="grid grid-cols-2 gap-3">
            {extras.map((extra) => {
              const Icon = extra.icon;
              return (
                <Card
                  key={extra.id}
                  className="p-4 border-none shadow-md cursor-pointer hover:shadow-lg transition-shadow"
                >
                  <div className="w-12 h-12 bg-[#D4AF37]/10 rounded-xl flex items-center justify-center mb-3">
                    <Icon className="w-6 h-6 text-[#D4AF37]" />
                  </div>
                  <h4 className="font-semibold text-[#0A1929] mb-1 text-sm">
                    {extra.name}
                  </h4>
                  <p className="text-[#D4AF37] font-bold">{extra.price}</p>
                </Card>
              );
            })}
          </div>
        </div>

        {/* Info Box */}
        <Card className="p-5 border-none shadow-md bg-[#D4AF37]/5">
          <h3 className="font-semibold text-[#0A1929] mb-2">
            💡 Dica
          </h3>
          <p className="text-sm text-gray-600">
            Combine serviços e extras para um cuidado completo do seu veículo.
            Extras podem ser adicionados durante a marcação.
          </p>
        </Card>
      </div>

      <BottomNav />
    </div>
  );
}
