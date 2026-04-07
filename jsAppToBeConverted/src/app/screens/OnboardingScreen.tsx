import { useState } from "react";
import { useNavigate } from "react-router";
import { Calendar, Sparkles, Gift, Star, ChevronRight } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { Button } from "../components/ui/button";

const onboardingSlides = [
  {
    icon: Calendar,
    title: "Marcação Simples",
    description: "Marque o seu serviço de lavagem em poucos passos. Rápido, fácil e conveniente.",
    color: "#D4AF37",
  },
  {
    icon: Sparkles,
    title: "Cuidado Premium",
    description: "Produtos de qualidade superior e acabamento impecável para o seu veículo.",
    color: "#D4AF37",
  },
  {
    icon: Gift,
    title: "Programa de Fidelização",
    description: "A cada 10 lavagens, ganhe 1 lavagem grátis. Acumule recompensas facilmente.",
    color: "#D4AF37",
  },
  {
    icon: Star,
    title: "Histórico e Avaliações",
    description: "Acompanhe o histórico das suas lavagens e ajude-nos a melhorar o serviço.",
    color: "#D4AF37",
  },
];

export default function OnboardingScreen() {
  const navigate = useNavigate();
  const [currentSlide, setCurrentSlide] = useState(0);

  const handleNext = () => {
    if (currentSlide < onboardingSlides.length - 1) {
      setCurrentSlide(currentSlide + 1);
    } else {
      navigate("/login");
    }
  };

  const handleSkip = () => {
    navigate("/login");
  };

  const slide = onboardingSlides[currentSlide];
  const Icon = slide.icon;

  return (
    <div className="h-screen w-full bg-white flex flex-col">
      <div className="flex-1 flex flex-col items-center justify-center px-6 pb-8">
        <AnimatePresence mode="wait">
          <motion.div
            key={currentSlide}
            initial={{ opacity: 0, x: 50 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -50 }}
            transition={{ duration: 0.3 }}
            className="flex flex-col items-center text-center"
          >
            <div className="relative mb-12">
              <div className="absolute inset-0 bg-[#D4AF37]/20 rounded-full blur-3xl"></div>
              <div className="relative bg-gradient-to-br from-[#0A1929] to-[#152C42] rounded-3xl p-12">
                <Icon className="w-24 h-24 text-[#D4AF37]" />
              </div>
            </div>

            <h2 className="text-3xl font-bold text-[#0A1929] mb-4">
              {slide.title}
            </h2>
            <p className="text-gray-600 text-lg leading-relaxed max-w-sm">
              {slide.description}
            </p>
          </motion.div>
        </AnimatePresence>
      </div>

      <div className="px-6 pb-12 space-y-6">
        <div className="flex justify-center gap-2">
          {onboardingSlides.map((_, index) => (
            <div
              key={index}
              className={`h-2 rounded-full transition-all duration-300 ${
                index === currentSlide
                  ? "w-8 bg-[#D4AF37]"
                  : "w-2 bg-gray-300"
              }`}
            />
          ))}
        </div>

        <div className="space-y-3">
          <Button
            onClick={handleNext}
            className="w-full h-14 bg-[#0A1929] hover:bg-[#152C42] text-white rounded-xl text-lg"
          >
            {currentSlide < onboardingSlides.length - 1 ? (
              <span className="flex items-center justify-center gap-2">
                Seguinte
                <ChevronRight className="w-5 h-5" />
              </span>
            ) : (
              "Começar"
            )}
          </Button>

          <Button
            onClick={handleSkip}
            variant="ghost"
            className="w-full h-12 text-gray-600 hover:text-[#0A1929]"
          >
            Saltar
          </Button>
        </div>
      </div>
    </div>
  );
}
