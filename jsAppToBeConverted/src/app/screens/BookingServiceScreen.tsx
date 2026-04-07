import { useState } from "react";
import { useNavigate } from "react-router";
import { Sparkles, Car, Droplets, Sofa, Clock, ArrowLeft, Check } from "lucide-react";
import { Card } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";

const services = [
  {
    id: "standard",
    name: "Lavagem Standard",
    description: "Lavagem completa exterior e interior",
    duration: "30 min",
    prices: { passenger: 25.0, suv: 27.0 },
    icon: Car,
  },
  {
    id: "premium",
    name: "Lavagem Premium",
    description: "Lavagem detalhada com acabamento premium",
    duration: "45 min",
    prices: { passenger: 32.0, suv: 34.0 },
    icon: Sparkles,
    popular: true,
  },
  {
    id: "exterior",
    name: "Lavagem Exterior",
    description: "Apenas lavagem exterior",
    duration: "20 min",
    prices: { passenger: 16.0, suv: 18.5 },
    icon: Droplets,
  },
  {
    id: "interior",
    name: "Limpeza do Interior",
    description: "Apenas limpeza interior",
    duration: "25 min",
    prices: { passenger: 16.0, suv: 18.5 },
    icon: Sofa,
  },
];

export default function BookingServiceScreen() {
  const navigate = useNavigate();
  const [selectedService, setSelectedService] = useState<string | null>(null);

  const handleContinue = () => {
    if (selectedService) {
      navigate("/booking/vehicle");
    }
  };

  return (
    <div className="min-h-screen w-full bg-gray-50 pb-32">
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
          Escolha o Serviço
        </h1>
        <p className="text-gray-400">Passo 1 de 4</p>
      </div>

      <div className="px-6 mt-6 space-y-3">
        {services.map((service) => {
          const Icon = service.icon;
          const isSelected = selectedService === service.id;

          return (
            <Card
              key={service.id}
              onClick={() => setSelectedService(service.id)}
              className={`p-5 border-2 cursor-pointer transition-all ${
                isSelected
                  ? "border-[#D4AF37] bg-[#D4AF37]/5 shadow-lg"
                  : "border-transparent shadow-md hover:shadow-lg"
              }`}
            >
              <div className="flex gap-4">
                <div
                  className={`w-16 h-16 rounded-2xl flex items-center justify-center flex-shrink-0 ${
                    isSelected ? "bg-[#D4AF37]" : "bg-[#D4AF37]/10"
                  }`}
                >
                  <Icon
                    className={`w-8 h-8 ${
                      isSelected ? "text-white" : "text-[#D4AF37]"
                    }`}
                  />
                </div>
                <div className="flex-1">
                  <div className="flex items-start justify-between mb-1">
                    <h3 className="font-bold text-[#0A1929]">{service.name}</h3>
                    {service.popular && (
                      <Badge className="bg-[#D4AF37] text-[#0A1929] border-none text-xs">
                        Popular
                      </Badge>
                    )}
                  </div>
                  <p className="text-sm text-gray-600 mb-3">
                    {service.description}
                  </p>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <span className="text-xs text-gray-500">A partir de</span>
                      <span className="text-lg font-bold text-[#D4AF37]">
                        {service.prices.passenger.toFixed(2).replace(".", ",")}€
                      </span>
                    </div>
                    <div className="flex items-center gap-1 text-gray-600">
                      <Clock className="w-4 h-4" />
                      <span className="text-sm">{service.duration}</span>
                    </div>
                  </div>
                </div>
                {isSelected && (
                  <div className="w-6 h-6 bg-[#D4AF37] rounded-full flex items-center justify-center flex-shrink-0">
                    <Check className="w-4 h-4 text-white" />
                  </div>
                )}
              </div>
            </Card>
          );
        })}
      </div>

      {/* Fixed Bottom Button */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-6">
        <Button
          onClick={handleContinue}
          disabled={!selectedService}
          className="w-full h-14 bg-[#0A1929] hover:bg-[#152C42] text-white rounded-xl text-lg font-semibold disabled:opacity-50"
        >
          Continuar
        </Button>
      </div>
    </div>
  );
}