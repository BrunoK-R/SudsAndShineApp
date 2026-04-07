import { useNavigate } from "react-router";
import {
  Calendar,
  Clock,
  MapPin,
  Star,
  Award,
  TrendingUp,
  Sparkles,
  Car,
  Shield,
  Zap,
  ChevronRight,
} from "lucide-react";
import { Button } from "../components/ui/button";
import { Card } from "../components/ui/card";
import { Progress } from "../components/ui/progress";
import { BottomNav } from "../components/BottomNav";

export default function HomeScreen() {
  const navigate = useNavigate();

  const featuredServices = [
    {
      name: "Lavagem Premium",
      price: "32,00€",
      duration: "45 min",
      icon: Sparkles,
    },
    {
      name: "Lavagem Standard",
      price: "25,00€",
      duration: "30 min",
      icon: Car,
    },
  ];

  return (
    <div className="min-h-screen w-full bg-gray-50 pb-24">
      {/* Header com gradiente */}
      <div className="bg-gradient-to-b from-[#0A1929] to-[#152C42] rounded-b-3xl pb-8 px-6 pt-12">
        <div className="flex items-start justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-white mb-1">
              Olá, João!
            </h1>
            <p className="text-gray-400">Bem-vindo de volta</p>
          </div>
          <button
            onClick={() => navigate("/profile")}
            className="w-12 h-12 bg-white/10 rounded-full flex items-center justify-center border border-white/20"
          >
            <span className="text-[#D4AF37] text-xl">JS</span>
          </button>
        </div>

        {/* CTA Principal */}
        <Button
          onClick={() => navigate("/booking/service")}
          className="w-full h-16 bg-[#D4AF37] hover:bg-[#B8982E] text-[#0A1929] rounded-2xl text-lg font-semibold flex items-center justify-between px-6"
        >
          <span className="flex items-center gap-3">
            <Calendar className="w-6 h-6" />
            Marcar Agora
          </span>
          <ChevronRight className="w-6 h-6" />
        </Button>
      </div>

      <div className="px-6 -mt-4 space-y-6">
        {/* Próxima Marcação */}
        <Card className="p-5 border-none shadow-lg">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold text-[#0A1929]">Próxima Marcação</h3>
            <span className="px-3 py-1 bg-[#D4AF37]/10 text-[#D4AF37] rounded-full text-xs font-semibold">
              Confirmado
            </span>
          </div>
          <div className="space-y-3">
            <div className="flex items-center gap-3 text-gray-600">
              <Sparkles className="w-5 h-5 text-[#D4AF37]" />
              <span>Lavagem Premium</span>
            </div>
            <div className="flex items-center gap-3 text-gray-600">
              <Calendar className="w-5 h-5 text-gray-400" />
              <span>25 de Março, 2026</span>
            </div>
            <div className="flex items-center gap-3 text-gray-600">
              <Clock className="w-5 h-5 text-gray-400" />
              <span>14:30</span>
            </div>
            <div className="flex items-center gap-3 text-gray-600">
              <MapPin className="w-5 h-5 text-gray-400" />
              <span className="text-sm">Shopping Norte Sul, Piso -1</span>
            </div>
          </div>
          <Button
            onClick={() => navigate("/bookings")}
            variant="outline"
            className="w-full mt-4 border-[#0A1929] text-[#0A1929] hover:bg-[#0A1929] hover:text-white rounded-xl"
          >
            Ver Detalhes
          </Button>
        </Card>

        {/* Progresso de Fidelização */}
        <Card className="p-5 border-none shadow-lg bg-gradient-to-br from-[#0A1929] to-[#152C42]">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold text-white">Programa de Fidelização</h3>
            <Award className="w-6 h-6 text-[#D4AF37]" />
          </div>
          <div className="mb-3">
            <div className="flex justify-between text-sm mb-2">
              <span className="text-gray-300">7 de 10 lavagens</span>
              <span className="text-[#D4AF37] font-semibold">3 restantes</span>
            </div>
            <Progress value={70} className="h-2 bg-white/10" />
          </div>
          <p className="text-gray-400 text-sm">
            Mais 3 lavagens para ganhar 1 lavagem grátis!
          </p>
        </Card>

        {/* Serviços em Destaque */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold text-[#0A1929]">Serviços em Destaque</h3>
            <button
              onClick={() => navigate("/services")}
              className="text-[#D4AF37] text-sm font-semibold flex items-center gap-1"
            >
              Ver Todos
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {featuredServices.map((service, index) => {
              const Icon = service.icon;
              return (
                <Card
                  key={index}
                  onClick={() => navigate("/booking/service")}
                  className="p-4 border-none shadow-md cursor-pointer hover:shadow-lg transition-shadow"
                >
                  <div className="w-12 h-12 bg-[#D4AF37]/10 rounded-xl flex items-center justify-center mb-3">
                    <Icon className="w-6 h-6 text-[#D4AF37]" />
                  </div>
                  <h4 className="font-semibold text-[#0A1929] mb-2 text-sm">
                    {service.name}
                  </h4>
                  <div className="flex items-center justify-between text-xs text-gray-600">
                    <span className="font-semibold text-[#D4AF37]">
                      {service.price}
                    </span>
                    <span className="flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      {service.duration}
                    </span>
                  </div>
                </Card>
              );
            })}
          </div>
        </div>

        {/* Estatísticas */}
        <div className="grid grid-cols-3 gap-3">
          <Card className="p-4 border-none shadow-md text-center">
            <div className="w-10 h-10 bg-[#D4AF37]/10 rounded-full flex items-center justify-center mx-auto mb-2">
              <TrendingUp className="w-5 h-5 text-[#D4AF37]" />
            </div>
            <p className="text-2xl font-bold text-[#0A1929]">500+</p>
            <p className="text-xs text-gray-600">Carros</p>
          </Card>
          <Card className="p-4 border-none shadow-md text-center">
            <div className="w-10 h-10 bg-[#D4AF37]/10 rounded-full flex items-center justify-center mx-auto mb-2">
              <Star className="w-5 h-5 text-[#D4AF37]" />
            </div>
            <p className="text-2xl font-bold text-[#0A1929]">4.9</p>
            <p className="text-xs text-gray-600">Avaliação</p>
          </Card>
          <Card className="p-4 border-none shadow-md text-center">
            <div className="w-10 h-10 bg-[#D4AF37]/10 rounded-full flex items-center justify-center mx-auto mb-2">
              <Award className="w-5 h-5 text-[#D4AF37]" />
            </div>
            <p className="text-2xl font-bold text-[#0A1929]">3+</p>
            <p className="text-xs text-gray-600">Anos</p>
          </Card>
        </div>

        {/* Benefícios */}
        <Card className="p-5 border-none shadow-md">
          <h3 className="font-semibold text-[#0A1929] mb-4">Por que escolher-nos?</h3>
          <div className="space-y-3">
            <div className="flex items-start gap-3">
              <div className="w-8 h-8 bg-[#D4AF37]/10 rounded-lg flex items-center justify-center flex-shrink-0">
                <Shield className="w-4 h-4 text-[#D4AF37]" />
              </div>
              <div>
                <p className="font-semibold text-sm text-[#0A1929]">
                  Acabamento Premium
                </p>
                <p className="text-xs text-gray-600">
                  Produtos de qualidade superior
                </p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <div className="w-8 h-8 bg-[#D4AF37]/10 rounded-lg flex items-center justify-center flex-shrink-0">
                <Zap className="w-4 h-4 text-[#D4AF37]" />
              </div>
              <div>
                <p className="font-semibold text-sm text-[#0A1929]">
                  Serviço Rápido
                </p>
                <p className="text-xs text-gray-600">Eficiente e pontual</p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <div className="w-8 h-8 bg-[#D4AF37]/10 rounded-lg flex items-center justify-center flex-shrink-0">
                <MapPin className="w-4 h-4 text-[#D4AF37]" />
              </div>
              <div>
                <p className="font-semibold text-sm text-[#0A1929]">
                  Localização Central
                </p>
                <p className="text-xs text-gray-600">Fácil acesso em Leiria</p>
              </div>
            </div>
          </div>
        </Card>
      </div>

      <BottomNav />
    </div>
  );
}
