import { useState } from "react";
import { useNavigate } from "react-router";
import {
  Calendar,
  Clock,
  MapPin,
  Sparkles,
  Car,
  CheckCircle,
  XCircle,
  MoreVertical,
} from "lucide-react";
import { Card } from "../components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../components/ui/tabs";
import { Badge } from "../components/ui/badge";
import { BottomNav } from "../components/BottomNav";

const bookings = {
  upcoming: [
    {
      id: 1,
      service: "Lavagem Premium",
      date: "25 de Março, 2026",
      time: "14:30",
      vehicle: "BMW 320d",
      price: "32,00€",
      status: "confirmed",
      icon: Sparkles,
    },
    {
      id: 2,
      service: "Lavagem Standard",
      date: "28 de Março, 2026",
      time: "10:00",
      vehicle: "VW Golf",
      price: "25,00€",
      status: "confirmed",
      icon: Car,
    },
  ],
  completed: [
    {
      id: 3,
      service: "Lavagem Premium",
      date: "15 de Março, 2026",
      time: "15:00",
      vehicle: "BMW 320d",
      price: "32,00€",
      status: "completed",
      icon: Sparkles,
    },
    {
      id: 4,
      service: "Lavagem Exterior",
      date: "10 de Março, 2026",
      time: "11:30",
      vehicle: "BMW 320d",
      price: "16,00€",
      status: "completed",
      icon: Car,
    },
  ],
};

export default function BookingsScreen() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("upcoming");

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "confirmed":
        return (
          <Badge className="bg-green-100 text-green-700 border-none">
            <CheckCircle className="w-3 h-3 mr-1" />
            Confirmado
          </Badge>
        );
      case "completed":
        return (
          <Badge className="bg-blue-100 text-blue-700 border-none">
            <CheckCircle className="w-3 h-3 mr-1" />
            Concluído
          </Badge>
        );
      case "cancelled":
        return (
          <Badge className="bg-red-100 text-red-700 border-none">
            <XCircle className="w-3 h-3 mr-1" />
            Cancelado
          </Badge>
        );
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen w-full bg-gray-50 pb-24">
      {/* Header */}
      <div className="bg-gradient-to-b from-[#0A1929] to-[#152C42] rounded-b-3xl pb-8 px-6 pt-12">
        <h1 className="text-3xl font-bold text-white mb-2">Marcações</h1>
        <p className="text-gray-400">Gerir as suas marcações</p>
      </div>

      <div className="px-6 -mt-4">
        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="w-full h-12 bg-white rounded-xl shadow-md p-1">
            <TabsTrigger
              value="upcoming"
              className="flex-1 h-10 rounded-lg data-[state=active]:bg-[#D4AF37] data-[state=active]:text-white"
            >
              Próximas
            </TabsTrigger>
            <TabsTrigger
              value="completed"
              className="flex-1 h-10 rounded-lg data-[state=active]:bg-[#D4AF37] data-[state=active]:text-white"
            >
              Concluídas
            </TabsTrigger>
          </TabsList>

          <TabsContent value="upcoming" className="mt-6 space-y-3">
            {bookings.upcoming.map((booking) => {
              const Icon = booking.icon;
              return (
                <Card
                  key={booking.id}
                  className="p-5 border-none shadow-md cursor-pointer hover:shadow-lg transition-shadow"
                  onClick={() => {}}
                >
                  <div className="flex justify-between items-start mb-4">
                    {getStatusBadge(booking.status)}
                    <button className="text-gray-400 hover:text-[#0A1929]">
                      <MoreVertical className="w-5 h-5" />
                    </button>
                  </div>

                  <div className="flex gap-4 mb-4">
                    <div className="w-14 h-14 bg-[#D4AF37]/10 rounded-xl flex items-center justify-center flex-shrink-0">
                      <Icon className="w-7 h-7 text-[#D4AF37]" />
                    </div>
                    <div className="flex-1">
                      <h3 className="font-bold text-[#0A1929] mb-1">
                        {booking.service}
                      </h3>
                      <p className="text-sm text-gray-600">{booking.vehicle}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-lg font-bold text-[#D4AF37]">
                        {booking.price}
                      </p>
                    </div>
                  </div>

                  <div className="space-y-2 text-sm text-gray-600">
                    <div className="flex items-center gap-2">
                      <Calendar className="w-4 h-4 text-[#D4AF37]" />
                      <span>{booking.date}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Clock className="w-4 h-4 text-[#D4AF37]" />
                      <span>{booking.time}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <MapPin className="w-4 h-4 text-[#D4AF37]" />
                      <span className="text-xs">
                        Shopping Norte Sul, Piso -1
                      </span>
                    </div>
                  </div>
                </Card>
              );
            })}
          </TabsContent>

          <TabsContent value="completed" className="mt-6 space-y-3">
            {bookings.completed.map((booking) => {
              const Icon = booking.icon;
              return (
                <Card
                  key={booking.id}
                  className="p-5 border-none shadow-md cursor-pointer hover:shadow-lg transition-shadow"
                  onClick={() => navigate("/rating")}
                >
                  <div className="flex justify-between items-start mb-4">
                    {getStatusBadge(booking.status)}
                    <button className="text-gray-400 hover:text-[#0A1929]">
                      <MoreVertical className="w-5 h-5" />
                    </button>
                  </div>

                  <div className="flex gap-4 mb-4">
                    <div className="w-14 h-14 bg-[#D4AF37]/10 rounded-xl flex items-center justify-center flex-shrink-0">
                      <Icon className="w-7 h-7 text-[#D4AF37]" />
                    </div>
                    <div className="flex-1">
                      <h3 className="font-bold text-[#0A1929] mb-1">
                        {booking.service}
                      </h3>
                      <p className="text-sm text-gray-600">{booking.vehicle}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-lg font-bold text-[#D4AF37]">
                        {booking.price}
                      </p>
                    </div>
                  </div>

                  <div className="space-y-2 text-sm text-gray-600">
                    <div className="flex items-center gap-2">
                      <Calendar className="w-4 h-4 text-[#D4AF37]" />
                      <span>{booking.date}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Clock className="w-4 h-4 text-[#D4AF37]" />
                      <span>{booking.time}</span>
                    </div>
                  </div>

                  <div className="mt-4 pt-4 border-t border-gray-200">
                    <button className="text-[#D4AF37] text-sm font-semibold hover:underline">
                      Avaliar Serviço →
                    </button>
                  </div>
                </Card>
              );
            })}
          </TabsContent>
        </Tabs>
      </div>

      <BottomNav />
    </div>
  );
}
