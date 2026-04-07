import { useState } from "react";
import { useNavigate } from "react-router";
import { Mail, Lock, Eye, EyeOff, Sparkles } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";

export default function LoginScreen() {
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = () => {
    // Simular login
    navigate("/home");
  };

  const handleGuestBooking = () => {
    navigate("/booking/service");
  };

  return (
    <div className="min-h-screen w-full bg-gradient-to-b from-[#0A1929] to-[#152C42] flex flex-col">
      <div className="flex-1 flex flex-col px-6 pt-16 pb-8">
        <div className="flex items-center justify-center mb-12">
          <div className="bg-white/10 backdrop-blur-sm rounded-2xl p-4 border border-white/20">
            <Sparkles className="w-12 h-12 text-[#D4AF37]" />
          </div>
        </div>

        <div className="mb-8">
          <h1 className="text-3xl font-bold text-white mb-2">Bem-vindo</h1>
          <p className="text-gray-400">Entre na sua conta para continuar</p>
        </div>

        <div className="space-y-5">
          <div className="space-y-2">
            <Label htmlFor="email" className="text-white">Email</Label>
            <div className="relative">
              <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <Input
                id="email"
                type="email"
                placeholder="seuemail@exemplo.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
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
                value={password}
                onChange={(e) => setPassword(e.target.value)}
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

          <button
            onClick={() => navigate("/forgot-password")}
            className="text-[#D4AF37] text-sm hover:underline"
          >
            Esqueceu a palavra-passe?
          </button>

          <Button
            onClick={handleLogin}
            className="w-full h-14 bg-[#D4AF37] hover:bg-[#B8982E] text-[#0A1929] rounded-xl text-lg font-semibold"
          >
            Entrar
          </Button>

          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-white/20"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-4 bg-[#0A1929] text-gray-400">ou</span>
            </div>
          </div>

          <Button
            onClick={handleGuestBooking}
            variant="outline"
            className="w-full h-14 bg-white border-2 border-white text-[#0A1929] hover:bg-gray-100 rounded-xl font-semibold"
          >
            Continuar como Convidado
          </Button>

          <div className="text-center mt-6">
            <span className="text-gray-400">Não tem conta? </span>
            <button
              onClick={() => navigate("/register")}
              className="text-[#D4AF37] font-semibold hover:underline"
            >
              Criar Conta
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}