import { useState } from "react";
import { useNavigate } from "react-router";
import { Calendar as CalendarIcon, Clock, ArrowLeft, Info } from "lucide-react";
import { Card } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Calendar } from "../components/ui/calendar";

const timeSlots = [
  { time: "09:00", available: true },
  { time: "09:30", available: true },
  { time: "10:00", available: false },
  { time: "10:30", available: true },
  { time: "11:00", available: true },
  { time: "11:30", available: true },
  { time: "12:00", available: true },
  { time: "12:30", available: false },
  { time: "14:00", available: true },
  { time: "14:30", available: true },
  { time: "15:00", available: true },
  { time: "15:30", available: true },
  { time: "16:00", available: true },
  { time: "16:30", available: true },
  { time: "17:00", available: true },
  { time: "17:30", available: true },
  { time: "18:00", available: true },
  { time: "18:30", available: true },
];

export default function BookingDateTimeScreen() {
  const navigate = useNavigate();
  const [selectedDate, setSelectedDate] = useState<Date | undefined>(new Date());
  const [selectedTime, setSelectedTime] = useState<string | null>(null);

  const handleContinue = () => {
    if (selectedDate && selectedTime) {
      navigate("/booking/contact");
    }
  };

  return (
    <div className="min-h-screen w-full bg-gray-50 pb-32">
      {/* Header */}
      <div className="bg-gradient-to-b from-[#0A1929] to-[#152C42] rounded-b-3xl pb-8 px-6 pt-12">
        <button
          onClick={() => navigate("/booking/vehicle")}
          className="flex items-center gap-2 text-[#D4AF37] mb-6"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Voltar</span>
        </button>
        <h1 className="text-3xl font-bold text-white mb-2">Data e Hora</h1>
        <p className="text-gray-400">Passo 3 de 4</p>
      </div>

      <div className="px-6 -mt-4 space-y-6">
        {/* Calendário */}
        <Card className="p-5 border-none shadow-lg">
          <div className="flex items-center gap-2 mb-4">
            <CalendarIcon className="w-5 h-5 text-[#D4AF37]" />
            <h3 className="font-semibold text-[#0A1929]">Selecione a Data</h3>
          </div>
          <Calendar
            mode="single"
            selected={selectedDate}
            onSelect={setSelectedDate}
            disabled={(date) => date < new Date() || date.getDay() === 0}
            className="rounded-md"
          />
        </Card>

        {/* Horários */}
        {selectedDate && (
          <Card className="p-5 border-none shadow-lg">
            <div className="flex items-center gap-2 mb-4">
              <Clock className="w-5 h-5 text-[#D4AF37]" />
              <h3 className="font-semibold text-[#0A1929]">
                Horários Disponíveis
              </h3>
            </div>
            <div className="grid grid-cols-3 gap-2">
              {timeSlots.map((slot) => (
                <button
                  key={slot.time}
                  onClick={() => slot.available && setSelectedTime(slot.time)}
                  disabled={!slot.available}
                  className={`h-12 rounded-xl font-semibold transition-all ${
                    selectedTime === slot.time
                      ? "bg-[#D4AF37] text-white"
                      : slot.available
                      ? "bg-gray-100 text-[#0A1929] hover:bg-gray-200"
                      : "bg-gray-100 text-gray-400 cursor-not-allowed opacity-50"
                  }`}
                >
                  {slot.time}
                </button>
              ))}
            </div>
          </Card>
        )}

        {/* Horário de Funcionamento */}
        <Card className="p-5 border-none shadow-md bg-[#D4AF37]/5">
          <div className="flex gap-3">
            <Info className="w-5 h-5 text-[#D4AF37] flex-shrink-0 mt-0.5" />
            <div>
              <h4 className="font-semibold text-[#0A1929] mb-2">
                Horário de Funcionamento
              </h4>
              <div className="space-y-1 text-sm text-gray-600">
                <p>Segunda a Sexta: 09:00 - 19:00</p>
                <p>Sábado: 09:00 - 13:00</p>
                <p>Domingo: Encerrado</p>
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* Fixed Bottom Button */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-6">
        <Button
          onClick={handleContinue}
          disabled={!selectedDate || !selectedTime}
          className="w-full h-14 bg-[#0A1929] hover:bg-[#152C42] text-white rounded-xl text-lg font-semibold disabled:opacity-50"
        >
          Continuar
        </Button>
      </div>
    </div>
  );
}
