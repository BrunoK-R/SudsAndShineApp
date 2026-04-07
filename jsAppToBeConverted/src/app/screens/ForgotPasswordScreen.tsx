import { useState } from "react";
import { useNavigate } from "react-router";
import { Mail, ArrowLeft, CheckCircle } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";

export default function ForgotPasswordScreen() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = () => {
    setSubmitted(true);
  };

  if (submitted) {
    return (
      <div className="min-h-screen w-full bg-gradient-to-b from-[#0A1929] to-[#152C42] flex flex-col items-center justify-center px-6">
        <div className="bg-white/10 backdrop-blur-sm rounded-3xl p-8 border border-white/20 mb-8">
          <CheckCircle className="w-20 h-20 text-[#D4AF37]" />
        </div>

        <h1 className="text-3xl font-bold text-white mb-4 text-center">
          Email Enviado
        </h1>
        <p className="text-gray-400 text-center mb-8 max-w-sm">
          Enviámos instruções para recuperar a sua palavra-passe para{" "}
          <span className="text-white font-semibold">{email}</span>
        </p>

        <Button
          onClick={() => navigate("/login")}
          className="w-full max-w-sm h-14 bg-[#D4AF37] hover:bg-[#B8982E] text-[#0A1929] rounded-xl text-lg font-semibold"
        >
          Voltar ao Login
        </Button>
      </div>
    );
  }

  return (
    <div className="min-h-screen w-full bg-gradient-to-b from-[#0A1929] to-[#152C42] flex flex-col">
      <div className="flex-1 flex flex-col px-6 pt-16 pb-8">
        <button
          onClick={() => navigate("/login")}
          className="flex items-center gap-2 text-[#D4AF37] mb-8"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Voltar</span>
        </button>

        <div className="mb-8">
          <h1 className="text-3xl font-bold text-white mb-2">
            Recuperar Palavra-passe
          </h1>
          <p className="text-gray-400">
            Introduza o seu email para receber instruções
          </p>
        </div>

        <div className="space-y-6">
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

          <Button
            onClick={handleSubmit}
            disabled={!email}
            className="w-full h-14 bg-[#D4AF37] hover:bg-[#B8982E] text-[#0A1929] rounded-xl text-lg font-semibold disabled:opacity-50"
          >
            Enviar Instruções
          </Button>
        </div>
      </div>
    </div>
  );
}
