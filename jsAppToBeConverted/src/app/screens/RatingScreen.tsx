import { useState } from "react";
import { useNavigate } from "react-router";
import { Star, ThumbsUp, Zap, Shield, Smile, ArrowLeft } from "lucide-react";
import { Card } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Textarea } from "../components/ui/textarea";
import { motion } from "motion/react";

const quickTags = [
  { id: "fast", label: "Rápido", icon: Zap },
  { id: "quality", label: "Qualidade", icon: Shield },
  { id: "friendly", label: "Simpático", icon: Smile },
  { id: "recommend", label: "Recomendo", icon: ThumbsUp },
];

export default function RatingScreen() {
  const navigate = useNavigate();
  const [rating, setRating] = useState(0);
  const [hoveredRating, setHoveredRating] = useState(0);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [comment, setComment] = useState("");
  const [submitted, setSubmitted] = useState(false);

  const handleTagToggle = (tagId: string) => {
    if (selectedTags.includes(tagId)) {
      setSelectedTags(selectedTags.filter((id) => id !== tagId));
    } else {
      setSelectedTags([...selectedTags, tagId]);
    }
  };

  const handleSubmit = () => {
    setSubmitted(true);
  };

  if (submitted) {
    return (
      <div className="min-h-screen w-full bg-gray-50 flex flex-col items-center justify-center px-6">
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ type: "spring", duration: 0.5 }}
          className="mb-8"
        >
          <div className="w-28 h-28 bg-gradient-to-br from-[#D4AF37] to-[#B8982E] rounded-full flex items-center justify-center">
            <Star className="w-16 h-16 text-white fill-white" />
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="text-center mb-8"
        >
          <h1 className="text-3xl font-bold text-[#0A1929] mb-3">
            Obrigado!
          </h1>
          <p className="text-gray-600">
            A sua avaliação ajuda-nos a melhorar o serviço
          </p>
        </motion.div>

        <Button
          onClick={() => navigate("/home")}
          className="w-full max-w-sm h-14 bg-[#D4AF37] hover:bg-[#B8982E] text-[#0A1929] rounded-xl text-lg font-semibold"
        >
          Voltar ao Início
        </Button>
      </div>
    );
  }

  return (
    <div className="min-h-screen w-full bg-gray-50 pb-32">
      {/* Header */}
      <div className="bg-gradient-to-b from-[#0A1929] to-[#152C42] rounded-b-3xl pb-8 px-6 pt-12">
        <button
          onClick={() => navigate("/bookings")}
          className="flex items-center gap-2 text-[#D4AF37] mb-6"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Voltar</span>
        </button>
        <h1 className="text-3xl font-bold text-white mb-2">
          Avaliar Serviço
        </h1>
        <p className="text-gray-400">Como foi a sua experiência?</p>
      </div>

      <div className="px-6 -mt-4 space-y-6">
        {/* Serviço Info */}
        <Card className="p-5 border-none shadow-lg">
          <p className="text-sm text-gray-600 mb-1">Serviço Realizado</p>
          <h3 className="text-xl font-bold text-[#0A1929]">Lavagem Premium</h3>
          <p className="text-sm text-gray-600">15 de Março, 2026</p>
        </Card>

        {/* Rating Stars */}
        <Card className="p-8 border-none shadow-lg">
          <h3 className="font-semibold text-[#0A1929] text-center mb-6">
            Classifique o Serviço
          </h3>
          <div className="flex justify-center gap-3 mb-2">
            {[1, 2, 3, 4, 5].map((star) => (
              <button
                key={star}
                onClick={() => setRating(star)}
                onMouseEnter={() => setHoveredRating(star)}
                onMouseLeave={() => setHoveredRating(0)}
                className="transition-transform hover:scale-110"
              >
                <Star
                  className={`w-12 h-12 ${
                    star <= (hoveredRating || rating)
                      ? "text-[#D4AF37] fill-[#D4AF37]"
                      : "text-gray-300"
                  }`}
                />
              </button>
            ))}
          </div>
          {rating > 0 && (
            <p className="text-center text-sm text-gray-600 mt-2">
              {rating === 5 && "Excelente!"}
              {rating === 4 && "Muito Bom!"}
              {rating === 3 && "Bom"}
              {rating === 2 && "Pode Melhorar"}
              {rating === 1 && "Insatisfeito"}
            </p>
          )}
        </Card>

        {/* Quick Tags */}
        {rating > 0 && (
          <Card className="p-6 border-none shadow-lg">
            <h3 className="font-semibold text-[#0A1929] mb-4">
              O que destacaria?
            </h3>
            <div className="grid grid-cols-2 gap-3">
              {quickTags.map((tag) => {
                const Icon = tag.icon;
                const isSelected = selectedTags.includes(tag.id);
                return (
                  <button
                    key={tag.id}
                    onClick={() => handleTagToggle(tag.id)}
                    className={`p-4 rounded-xl border-2 transition-all ${
                      isSelected
                        ? "bg-[#D4AF37]/10 border-[#D4AF37]"
                        : "bg-white border-gray-200 hover:border-[#D4AF37]"
                    }`}
                  >
                    <Icon
                      className={`w-6 h-6 mb-2 mx-auto ${
                        isSelected ? "text-[#D4AF37]" : "text-gray-400"
                      }`}
                    />
                    <p
                      className={`text-sm font-semibold ${
                        isSelected ? "text-[#D4AF37]" : "text-gray-600"
                      }`}
                    >
                      {tag.label}
                    </p>
                  </button>
                );
              })}
            </div>
          </Card>
        )}

        {/* Comment */}
        {rating > 0 && (
          <Card className="p-6 border-none shadow-lg">
            <h3 className="font-semibold text-[#0A1929] mb-4">
              Comentário (Opcional)
            </h3>
            <Textarea
              placeholder="Partilhe mais detalhes sobre a sua experiência..."
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              className="min-h-32 bg-gray-50 border-gray-200 rounded-xl resize-none"
            />
          </Card>
        )}
      </div>

      {/* Fixed Bottom Button */}
      {rating > 0 && (
        <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-6">
          <Button
            onClick={handleSubmit}
            className="w-full h-14 bg-[#D4AF37] hover:bg-[#B8982E] text-[#0A1929] rounded-xl text-lg font-semibold"
          >
            Enviar Avaliação
          </Button>
        </div>
      )}
    </div>
  );
}
