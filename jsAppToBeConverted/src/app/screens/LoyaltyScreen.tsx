import { useNavigate } from "react-router";
import { Gift, Sparkles, Trophy, TrendingUp, Calendar } from "lucide-react";
import { Card } from "../components/ui/card";
import { Progress } from "../components/ui/progress";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";
import { BottomNav } from "../components/BottomNav";

export default function LoyaltyScreen() {
  const navigate = useNavigate();

  const currentWashes = 7;
  const targetWashes = 10;
  const progress = (currentWashes / targetWashes) * 100;

  const rewards = [
    {
      id: 1,
      title: "Lavagem Grátis",
      description: "Complete 10 lavagens",
      current: 7,
      target: 10,
      icon: Gift,
    },
  ];

  const history = [
    { date: "15 de Março, 2026", service: "Lavagem Premium", points: 1 },
    { date: "10 de Março, 2026", service: "Lavagem Exterior", points: 1 },
    { date: "5 de Março, 2026", service: "Lavagem Standard", points: 1 },
    { date: "28 de Fevereiro, 2026", service: "Lavagem Premium", points: 1 },
    { date: "20 de Fevereiro, 2026", service: "Limpeza Interior", points: 1 },
    { date: "15 de Fevereiro, 2026", service: "Lavagem Standard", points: 1 },
    { date: "8 de Fevereiro, 2026", service: "Lavagem Premium", points: 1 },
  ];

  return (
    <div className="min-h-screen w-full bg-gray-50 pb-24">
      {/* Header */}
      <div className="bg-gradient-to-b from-[#0A1929] to-[#152C42] rounded-b-3xl pb-8 px-6 pt-12">
        <h1 className="text-3xl font-bold text-white mb-2">Recompensas</h1>
        <p className="text-gray-400">Acompanhe o seu progresso</p>
      </div>

      <div className="px-6 -mt-4 space-y-6">
        {/* Progresso Principal */}
        <Card className="p-6 border-none shadow-lg bg-gradient-to-br from-[#D4AF37] to-[#B8982E] overflow-hidden relative">
          <div className="absolute top-0 right-0 opacity-10">
            <Trophy className="w-40 h-40 text-white" />
          </div>
          <div className="relative z-10">
            <div className="flex items-center gap-2 mb-4">
              <Gift className="w-6 h-6 text-white" />
              <h2 className="text-xl font-bold text-white">
                Programa de Fidelização
              </h2>
            </div>

            <div className="mb-4">
              <div className="flex justify-between items-end mb-3">
                <div>
                  <p className="text-white/80 text-sm mb-1">Progresso Atual</p>
                  <p className="text-4xl font-bold text-white">
                    {currentWashes}/{targetWashes}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-white/80 text-sm mb-1">Faltam</p>
                  <p className="text-3xl font-bold text-white">
                    {targetWashes - currentWashes}
                  </p>
                </div>
              </div>
              <Progress
                value={progress}
                className="h-3 bg-white/20"
              />
            </div>

            <p className="text-white/90 text-sm">
              🎉 Mais {targetWashes - currentWashes} lavagens para ganhar 1
              lavagem grátis!
            </p>
          </div>
        </Card>

        {/* Selos de Progresso */}
        <Card className="p-6 border-none shadow-lg">
          <h3 className="font-semibold text-[#0A1929] mb-4">Seus Selos</h3>
          <div className="grid grid-cols-5 gap-3">
            {Array.from({ length: targetWashes }).map((_, index) => {
              const isEarned = index < currentWashes;
              return (
                <div
                  key={index}
                  className={`aspect-square rounded-2xl flex items-center justify-center border-2 transition-all ${
                    isEarned
                      ? "bg-[#D4AF37] border-[#D4AF37]"
                      : "bg-gray-100 border-gray-200"
                  }`}
                >
                  {isEarned ? (
                    <Sparkles className="w-8 h-8 text-white" />
                  ) : (
                    <span className="text-gray-400 font-bold">{index + 1}</span>
                  )}
                </div>
              );
            })}
          </div>
        </Card>

        {/* Como Funciona */}
        <Card className="p-6 border-none shadow-lg">
          <div className="flex items-center gap-2 mb-4">
            <TrendingUp className="w-5 h-5 text-[#D4AF37]" />
            <h3 className="font-semibold text-[#0A1929]">Como Funciona</h3>
          </div>
          <div className="space-y-3">
            <div className="flex gap-3">
              <div className="w-8 h-8 bg-[#D4AF37]/10 rounded-full flex items-center justify-center flex-shrink-0">
                <span className="text-[#D4AF37] font-bold text-sm">1</span>
              </div>
              <div>
                <p className="font-semibold text-[#0A1929] text-sm">
                  Faça Lavagens
                </p>
                <p className="text-xs text-gray-600">
                  Cada lavagem conta como 1 selo
                </p>
              </div>
            </div>
            <div className="flex gap-3">
              <div className="w-8 h-8 bg-[#D4AF37]/10 rounded-full flex items-center justify-center flex-shrink-0">
                <span className="text-[#D4AF37] font-bold text-sm">2</span>
              </div>
              <div>
                <p className="font-semibold text-[#0A1929] text-sm">
                  Acumule Selos
                </p>
                <p className="text-xs text-gray-600">
                  Junte 10 selos no total
                </p>
              </div>
            </div>
            <div className="flex gap-3">
              <div className="w-8 h-8 bg-[#D4AF37]/10 rounded-full flex items-center justify-center flex-shrink-0">
                <span className="text-[#D4AF37] font-bold text-sm">3</span>
              </div>
              <div>
                <p className="font-semibold text-[#0A1929] text-sm">
                  Ganhe Recompensa
                </p>
                <p className="text-xs text-gray-600">
                  Receba 1 lavagem grátis!
                </p>
              </div>
            </div>
          </div>
        </Card>

        {/* Histórico de Selos */}
        <Card className="p-6 border-none shadow-lg">
          <h3 className="font-semibold text-[#0A1929] mb-4">
            Histórico de Selos
          </h3>
          <div className="space-y-3">
            {history.map((item, index) => (
              <div
                key={index}
                className="flex items-center justify-between py-2 border-b border-gray-100 last:border-0"
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-[#D4AF37]/10 rounded-lg flex items-center justify-center">
                    <Sparkles className="w-5 h-5 text-[#D4AF37]" />
                  </div>
                  <div>
                    <p className="font-semibold text-[#0A1929] text-sm">
                      {item.service}
                    </p>
                    <p className="text-xs text-gray-600 flex items-center gap-1">
                      <Calendar className="w-3 h-3" />
                      {item.date}
                    </p>
                  </div>
                </div>
                <Badge className="bg-[#D4AF37]/10 text-[#D4AF37] border-none">
                  +{item.points} selo
                </Badge>
              </div>
            ))}
          </div>
        </Card>

        {/* CTA */}
        <Button
          onClick={() => navigate("/booking/service")}
          className="w-full h-14 bg-[#0A1929] hover:bg-[#152C42] text-white rounded-xl text-lg font-semibold"
        >
          Marcar Nova Lavagem
        </Button>
      </div>

      <BottomNav />
    </div>
  );
}
