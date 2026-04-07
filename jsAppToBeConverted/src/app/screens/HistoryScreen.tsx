import { ArrowLeft, Calendar, CheckCircle } from "lucide-react";
import { useNavigate } from "react-router";
import { Card } from "../components/ui/card";
import { Badge } from "../components/ui/badge";

const history = [
  {
    id: 1,
    service: "Lavagem Premium",
    date: "15 de Março, 2026",
    vehicle: "BMW 320d",
    price: "32,00€",
    status: "completed",
  },
  {
    id: 2,
    service: "Lavagem Exterior",
    date: "10 de Março, 2026",
    vehicle: "BMW 320d",
    price: "16,00€",
    status: "completed",
  },
  {
    id: 3,
    service: "Lavagem Standard",
    date: "5 de Março, 2026",
    vehicle: "VW Golf",
    price: "25,00€",
    status: "completed",
  },
  {
    id: 4,
    service: "Lavagem Premium",
    date: "28 de Fevereiro, 2026",
    vehicle: "BMW 320d",
    price: "32,00€",
    status: "completed",
  },
  {
    id: 5,
    service: "Limpeza do Interior",
    date: "20 de Fevereiro, 2026",
    vehicle: "VW Golf",
    price: "16,00€",
    status: "completed",
  },
  {
    id: 6,
    service: "Lavagem Standard",
    date: "15 de Fevereiro, 2026",
    vehicle: "BMW 320d",
    price: "25,00€",
    status: "completed",
  },
  {
    id: 7,
    service: "Lavagem Premium",
    date: "8 de Fevereiro, 2026",
    vehicle: "VW Golf",
    price: "32,00€",
    status: "completed",
  },
];

export default function HistoryScreen() {
  const navigate = useNavigate();

  const totalSpent = history.reduce((sum, item) => {
    return sum + parseFloat(item.price.replace("€", "").replace(",", "."));
  }, 0);

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
          Histórico de Lavagens
        </h1>
        <p className="text-gray-400">Todas as suas lavagens anteriores</p>
      </div>

      <div className="px-6 -mt-4 space-y-4">
        {/* Summary Card */}
        <Card className="p-6 border-none shadow-lg bg-gradient-to-br from-[#D4AF37] to-[#B8982E]">
          <div className="grid grid-cols-2 gap-4 text-white">
            <div>
              <p className="text-white/80 text-sm mb-1">Total de Lavagens</p>
              <p className="text-3xl font-bold">{history.length}</p>
            </div>
            <div>
              <p className="text-white/80 text-sm mb-1">Total Investido</p>
              <p className="text-3xl font-bold">
                {totalSpent.toFixed(2).replace(".", ",")}€
              </p>
            </div>
          </div>
        </Card>

        {/* History List */}
        <div className="space-y-3">
          {history.map((item) => (
            <Card
              key={item.id}
              className="p-5 border-none shadow-md hover:shadow-lg transition-shadow"
            >
              <div className="flex justify-between items-start mb-3">
                <Badge className="bg-blue-100 text-blue-700 border-none text-xs">
                  <CheckCircle className="w-3 h-3 mr-1" />
                  Concluído
                </Badge>
                <p className="text-lg font-bold text-[#D4AF37]">{item.price}</p>
              </div>

              <h3 className="font-bold text-[#0A1929] mb-1">{item.service}</h3>
              <p className="text-sm text-gray-600 mb-3">{item.vehicle}</p>

              <div className="flex items-center gap-2 text-sm text-gray-600">
                <Calendar className="w-4 h-4 text-[#D4AF37]" />
                <span>{item.date}</span>
              </div>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
