import { useState } from "react";
import { useNavigate } from "react-router";
import { Car, ArrowLeft, Check } from "lucide-react";
import { Card } from "../components/ui/card";
import { Button } from "../components/ui/button";

const vehicleTypes = [
  {
    id: "passenger",
    name: "Passageiros",
    description: "Carros normais, sedans, compactos",
    icon: "🚗",
    priceMultiplier: 1,
  },
  {
    id: "suv",
    name: "SUV",
    description: "SUVs, vans, carrinhas",
    icon: "🚙",
    priceMultiplier: 1.08,
  },
];

export default function BookingVehicleScreen() {
  const navigate = useNavigate();
  const [selectedVehicle, setSelectedVehicle] = useState<string | null>(null);

  const handleContinue = () => {
    if (selectedVehicle) {
      navigate("/booking/datetime");
    }
  };

  return (
    <div className="min-h-screen w-full bg-gray-50 pb-32">
      {/* Header */}
      <div className="bg-gradient-to-b from-[#0A1929] to-[#152C42] rounded-b-3xl pb-8 px-6 pt-12">
        <button
          onClick={() => navigate("/booking/service")}
          className="flex items-center gap-2 text-[#D4AF37] mb-6"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Voltar</span>
        </button>
        <h1 className="text-3xl font-bold text-white mb-2">
          Tipo de Veículo
        </h1>
        <p className="text-gray-400">Passo 2 de 4</p>
      </div>

      <div className="px-6 -mt-4 space-y-4">
        <p className="text-sm mb-4 text-[#cfd5e1]">
          Selecione o tipo de veículo para calcular o preço correto
        </p>

        {vehicleTypes.map((vehicle) => {
          const isSelected = selectedVehicle === vehicle.id;

          return (
            <Card
              key={vehicle.id}
              onClick={() => setSelectedVehicle(vehicle.id)}
              className={`p-6 border-2 cursor-pointer transition-all ${
                isSelected
                  ? "border-[#D4AF37] bg-[#D4AF37]/5 shadow-lg"
                  : "border-transparent shadow-md hover:shadow-lg"
              }`}
            >
              <div className="flex items-center gap-4">
                <div
                  className={`w-20 h-20 rounded-2xl flex items-center justify-center flex-shrink-0 text-4xl ${
                    isSelected ? "bg-[#D4AF37]" : "bg-[#D4AF37]/10"
                  }`}
                >
                  {vehicle.icon}
                </div>
                <div className="flex-1">
                  <h3 className="text-xl font-bold text-[#0A1929] mb-1">
                    {vehicle.name}
                  </h3>
                  <p className="text-sm text-gray-600">{vehicle.description}</p>
                </div>
                {isSelected && (
                  <div className="w-8 h-8 bg-[#D4AF37] rounded-full flex items-center justify-center flex-shrink-0">
                    <Check className="w-5 h-5 text-white" />
                  </div>
                )}
              </div>
            </Card>
          );
        })}

        {/* Info Box */}
        <Card className="p-5 border-none shadow-md bg-blue-50">
          <div className="flex gap-3">
            <Car className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
            <div>
              <h4 className="font-semibold text-blue-900 mb-1">
                Não tem a certeza?
              </h4>
              <p className="text-sm text-blue-800">
                Carros SUV incluem veículos maiores como SUVs, vans e carrinhas.
                O preço é ligeiramente superior devido ao tamanho.
              </p>
            </div>
          </div>
        </Card>
      </div>

      {/* Fixed Bottom Button */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-6">
        <Button
          onClick={handleContinue}
          disabled={!selectedVehicle}
          className="w-full h-14 bg-[#0A1929] hover:bg-[#152C42] text-white rounded-xl text-lg font-semibold disabled:opacity-50"
        >
          Continuar
        </Button>
      </div>
    </div>
  );
}
