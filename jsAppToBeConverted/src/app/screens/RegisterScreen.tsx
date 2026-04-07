import { useState } from "react";
import { useNavigate } from "react-router";
import { User, Mail, Lock, Eye, EyeOff, Phone, Sparkles } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Checkbox } from "../components/ui/checkbox";

export default function RegisterScreen() {
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    phone: "",
    password: "",
    acceptTerms: false,
  });

  const handleRegister = () => {
    // Simular registo
    navigate("/home");
  };

  return (
    <div className="min-h-screen w-full bg-gradient-to-b from-[#0A1929] to-[#152C42] flex flex-col">
      <div className="flex-1 flex flex-col px-6 pt-16 pb-8">
        <div className="flex items-center justify-center mb-8">
          <div className="bg-white/10 backdrop-blur-sm rounded-2xl p-4 border border-white/20">
            <Sparkles className="w-12 h-12 text-[#D4AF37]" />
          </div>
        </div>

        <div className="mb-6">
          <h1 className="text-3xl font-bold text-white mb-2">Criar Conta</h1>
          <p className="text-gray-400">Registe-se para começar</p>
        </div>

        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="name" className="text-white">Nome Completo</Label>
            <div className="relative">
              <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <Input
                id="name"
                type="text"
                placeholder="João Silva"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="pl-12 h-14 bg-white/10 border-white/20 text-white placeholder:text-gray-400 rounded-xl"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="email" className="text-white">Email</Label>
            <div className="relative">
              <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <Input
                id="email"
                type="email"
                placeholder="seuemail@exemplo.com"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                className="pl-12 h-14 bg-white/10 border-white/20 text-white placeholder:text-gray-400 rounded-xl"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="phone" className="text-white">Telemóvel</Label>
            <div className="relative">
              <Phone className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <Input
                id="phone"
                type="tel"
                placeholder="913 005 855"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                className="pl-12 h-14 bg-white/10 border-white/20 text-white placeholder:text-gray-400 rounded-xl"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="password" className="text-white">Palavra-passe</Label>
            <div className="relative">
              <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <Input
                id="password"
                type={showPassword ? "text" : "password"}
                placeholder="••••••••"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                className="pl-12 pr-12 h-14 bg-white/10 border-white/20 text-white placeholder:text-gray-400 rounded-xl"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white"
              >
                {showPassword ? (
                  <EyeOff className="w-5 h-5" />
                ) : (
                  <Eye className="w-5 h-5" />
                )}
              </button>
            </div>
          </div>

          <div className="flex items-start gap-3 py-2">
            <Checkbox
              id="terms"
              checked={formData.acceptTerms}
              onCheckedChange={(checked) =>
                setFormData({ ...formData, acceptTerms: checked as boolean })
              }
              className="mt-1 border-white/20 data-[state=checked]:bg-[#D4AF37] data-[state=checked]:border-[#D4AF37]"
            />
            <label htmlFor="terms" className="text-sm text-gray-400 leading-relaxed">
              Aceito a{" "}
              <button className="text-[#D4AF37] hover:underline">
                Política de Privacidade
              </button>{" "}
              e os{" "}
              <button className="text-[#D4AF37] hover:underline">
                Termos de Serviço
              </button>
            </label>
          </div>

          <Button
            onClick={handleRegister}
            disabled={!formData.acceptTerms}
            className="w-full h-14 bg-[#D4AF37] hover:bg-[#B8982E] text-[#0A1929] rounded-xl text-lg font-semibold disabled:opacity-50"
          >
            Criar Conta
          </Button>

          <div className="text-center mt-6">
            <span className="text-gray-400">Já tem conta? </span>
            <button
              onClick={() => navigate("/login")}
              className="text-[#D4AF37] font-semibold hover:underline"
            >
              Entrar
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
