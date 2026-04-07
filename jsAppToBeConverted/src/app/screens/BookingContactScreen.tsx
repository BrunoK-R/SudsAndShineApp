import { useState } from "react";
import { useNavigate } from "react-router";
import { User, Phone, Mail, MessageSquare, ArrowLeft } from "lucide-react";
import { Card } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Textarea } from "../components/ui/textarea";
import { Checkbox } from "../components/ui/checkbox";

export default function BookingContactScreen() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    name: "",
    phone: "",
    email: "",
    notes: "",
    acceptPrivacy: false,
  });

  const handleContinue = () => {
    if (
      formData.name &&
      formData.phone &&
      formData.email &&
      formData.acceptPrivacy
    ) {
      navigate("/booking/confirmation");
    }
  };

  const isFormValid =
    formData.name &&
    formData.phone &&
    formData.email &&
    formData.acceptPrivacy;

  return (
    <div className="min-h-screen w-full bg-gray-50 pb-32">
      {/* Header */}
      <div className="bg-gradient-to-b from-[#0A1929] to-[#152C42] rounded-b-3xl pb-8 px-6 pt-12">
        <button
          onClick={() => navigate("/booking/datetime")}
          className="flex items-center gap-2 text-[#D4AF37] mb-6"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Voltar</span>
        </button>
        <h1 className="text-3xl font-bold text-white mb-2">
          Dados de Contacto
        </h1>
        <p className="text-gray-400">Passo 4 de 4</p>
      </div>

      <div className="px-6 -mt-4 space-y-5">
        <Card className="p-6 border-none shadow-lg space-y-5">
          <div className="space-y-2">
            <Label htmlFor="name" className="text-[#0A1929]">
              Nome Completo *
            </Label>
            <div className="relative">
              <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <Input
                id="name"
                type="text"
                placeholder="João Silva"
                value={formData.name}
                onChange={(e) =>
                  setFormData({ ...formData, name: e.target.value })
                }
                className="pl-12 h-12 bg-gray-50 border-gray-200 rounded-xl"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="phone" className="text-[#0A1929]">
              Telemóvel *
            </Label>
            <div className="relative">
              <Phone className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <Input
                id="phone"
                type="tel"
                placeholder="913 005 855"
                value={formData.phone}
                onChange={(e) =>
                  setFormData({ ...formData, phone: e.target.value })
                }
                className="pl-12 h-12 bg-gray-50 border-gray-200 rounded-xl"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="email" className="text-[#0A1929]">
              Email *
            </Label>
            <div className="relative">
              <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <Input
                id="email"
                type="email"
                placeholder="seuemail@exemplo.com"
                value={formData.email}
                onChange={(e) =>
                  setFormData({ ...formData, email: e.target.value })
                }
                className="pl-12 h-12 bg-gray-50 border-gray-200 rounded-xl"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="notes" className="text-[#0A1929]">
              Observações (Opcional)
            </Label>
            <div className="relative">
              <MessageSquare className="absolute left-4 top-4 w-5 h-5 text-gray-400" />
              <Textarea
                id="notes"
                placeholder="Alguma informação adicional que devamos saber..."
                value={formData.notes}
                onChange={(e) =>
                  setFormData({ ...formData, notes: e.target.value })
                }
                className="pl-12 pt-3 min-h-24 bg-gray-50 border-gray-200 rounded-xl resize-none"
              />
            </div>
          </div>
        </Card>

        <Card className="p-5 border-none shadow-md">
          <div className="flex items-start gap-3">
            <Checkbox
              id="privacy"
              checked={formData.acceptPrivacy}
              onCheckedChange={(checked) =>
                setFormData({ ...formData, acceptPrivacy: checked as boolean })
              }
              className="mt-1 border-gray-300 data-[state=checked]:bg-[#D4AF37] data-[state=checked]:border-[#D4AF37]"
            />
            <label htmlFor="privacy" className="text-sm text-gray-600 leading-relaxed">
              Aceito a{" "}
              <button className="text-[#D4AF37] font-semibold hover:underline">
                Política de Privacidade
              </button>{" "}
              e autorizo o processamento dos meus dados para efeitos de marcação.
              *
            </label>
          </div>
        </Card>
      </div>

      {/* Fixed Bottom Button */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-6">
        <Button
          onClick={handleContinue}
          disabled={!isFormValid}
          className="w-full h-14 bg-[#0A1929] hover:bg-[#152C42] text-white rounded-xl text-lg font-semibold disabled:opacity-50"
        >
          Rever Marcação
        </Button>
      </div>
    </div>
  );
}
