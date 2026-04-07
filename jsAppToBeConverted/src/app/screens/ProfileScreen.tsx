import { useNavigate } from "react-router";
import {
  User,
  MapPin,
  Car,
  Gift,
  Bell,
  Calendar,
  LogOut,
  ChevronRight,
  HelpCircle,
  Shield,
} from "lucide-react";
import { Card } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Switch } from "../components/ui/switch";
import { Separator } from "../components/ui/separator";
import { BottomNav } from "../components/BottomNav";

export default function ProfileScreen() {
  const navigate = useNavigate();

  const menuItems = [
    {
      icon: User,
      label: "Dados Pessoais",
      action: () => {},
    },
    {
      icon: Car,
      label: "Meus Veículos",
      action: () => navigate("/vehicles"),
    },
    {
      icon: Gift,
      label: "Programa de Fidelização",
      action: () => navigate("/loyalty"),
    },
    {
      icon: Calendar,
      label: "Histórico de Lavagens",
      action: () => navigate("/history"),
    },
    {
      icon: Bell,
      label: "Notificações",
      action: () => {},
    },
    {
      icon: HelpCircle,
      label: "Ajuda e Suporte",
      action: () => navigate("/contact"),
    },
    {
      icon: Shield,
      label: "Privacidade",
      action: () => {},
    },
  ];

  return (
    <div className="min-h-screen w-full bg-gray-50 pb-24">
      {/* Header */}
      <div className="bg-gradient-to-b from-[#0A1929] to-[#152C42] rounded-b-3xl pb-12 px-6 pt-12">
        <div className="flex items-center gap-4 mb-6">
          <div className="w-20 h-20 bg-[#D4AF37] rounded-full flex items-center justify-center">
            <span className="text-white text-2xl font-bold">JS</span>
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white mb-1">João Silva</h1>
            <p className="text-gray-400">joao.silva@exemplo.com</p>
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-3">
          <Card className="p-4 border-none text-center bg-white/10 backdrop-blur-sm">
            <p className="text-2xl font-bold text-white mb-1">7</p>
            <p className="text-xs text-gray-400">Lavagens</p>
          </Card>
          <Card className="p-4 border-none text-center bg-white/10 backdrop-blur-sm">
            <p className="text-2xl font-bold text-[#D4AF37] mb-1">3</p>
            <p className="text-xs text-gray-400">Faltam</p>
          </Card>
          <Card className="p-4 border-none text-center bg-white/10 backdrop-blur-sm">
            <p className="text-2xl font-bold text-white mb-1">2</p>
            <p className="text-xs text-gray-400">Veículos</p>
          </Card>
        </div>
      </div>

      <div className="px-6 -mt-8 space-y-4">
        {/* Dados de Contacto */}
        <Card className="p-6 border-none shadow-lg">
          <h3 className="font-semibold text-[#0A1929] mb-4">
            Suds & Shine mais próximo:
          </h3>
          <div className="space-y-3">
            <div className="flex items-start gap-3">
              <div className="w-10 h-10 bg-[#D4AF37]/10 rounded-xl flex items-center justify-center flex-shrink-0">
                <MapPin className="w-5 h-5 text-[#D4AF37]" />
              </div>
              <div className="flex-1">
                <p className="font-semibold text-[#0A1929] text-sm mb-1">
                  Morada
                </p>
                <p className="text-sm text-gray-600 mb-3">
                  R. Virgílio Vieira da Cunha, 2400-447 Leiria
                </p>
                <Button
                  onClick={() => window.open("https://www.google.com/maps/place/Suds+%26+Shine+Solutions/@39.7463887,-8.8257419,17z", "_blank")}
                  className="w-full bg-[#D4AF37] hover:bg-[#B8982E] text-[#0A1929] rounded-xl h-10 font-semibold"
                >
                  Navegar até
                </Button>
              </div>
            </div>
            
            {/* Mapa Integrado */}
            <div className="mt-4 rounded-xl overflow-hidden">
              <iframe 
                src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d5104.844570655629!2d-8.82574190497596!3d39.74638869821298!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0xd2273f8657fcfd3%3A0xa5aba894c893cf37!2sSuds%20%26%20Shine%20Solutions!5e1!3m2!1sen!2spt!4v1773940232354!5m2!1sen!2spt"
                width="100%"
                height="250"
                style={{ border: 0 }}
                allowFullScreen={true}
                loading="lazy"
                referrerPolicy="no-referrer-when-downgrade"
                className="w-full"
              />
            </div>
          </div>
        </Card>

        {/* Menu Items */}
        <Card className="border-none shadow-lg divide-y divide-gray-100">
          {menuItems.map((item, index) => {
            const Icon = item.icon;
            return (
              <button
                key={index}
                onClick={item.action}
                className="w-full flex items-center justify-between p-5 hover:bg-gray-50 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-[#D4AF37]/10 rounded-xl flex items-center justify-center">
                    <Icon className="w-5 h-5 text-[#D4AF37]" />
                  </div>
                  <span className="font-semibold text-[#0A1929]">
                    {item.label}
                  </span>
                </div>
                <ChevronRight className="w-5 h-5 text-gray-400" />
              </button>
            );
          })}
        </Card>

        {/* Preferências */}
        <Card className="p-6 border-none shadow-lg">
          <h3 className="font-semibold text-[#0A1929] mb-4">Preferências</h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-semibold text-[#0A1929] text-sm">
                  Notificações Push
                </p>
                <p className="text-xs text-gray-600">
                  Receber lembretes de marcações
                </p>
              </div>
              <Switch defaultChecked />
            </div>
            <Separator />
            <div className="flex items-center justify-between">
              <div>
                <p className="font-semibold text-[#0A1929] text-sm">
                  Email Marketing
                </p>
                <p className="text-xs text-gray-600">
                  Receber ofertas e promoções
                </p>
              </div>
              <Switch defaultChecked />
            </div>
          </div>
        </Card>

        {/* Logout */}
        <Button
          onClick={() => navigate("/login")}
          variant="outline"
          className="w-full h-14 border-red-600 text-red-600 hover:bg-red-600 hover:text-white rounded-xl"
        >
          <LogOut className="w-5 h-5 mr-2" />
          Terminar Sessão
        </Button>

        <p className="text-center text-xs text-gray-500 py-4">
          Versão 1.0.0 • Suds & Shine Solutions
        </p>
      </div>

      <BottomNav />
    </div>
  );
}