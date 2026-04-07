import { useState } from "react";
import { useNavigate } from "react-router";
import { Car, Plus, Edit, Trash2, ArrowLeft } from "lucide-react";
import { Card } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "../components/ui/dialog";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../components/ui/select";

const initialVehicles = [
  {
    id: 1,
    brand: "BMW",
    model: "320d",
    plate: "AA-00-BB",
    color: "Preto",
    type: "passenger",
  },
  {
    id: 2,
    brand: "Volkswagen",
    model: "Golf",
    plate: "CC-11-DD",
    color: "Branco",
    type: "passenger",
  },
];

export default function VehiclesScreen() {
  const navigate = useNavigate();
  const [vehicles, setVehicles] = useState(initialVehicles);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [newVehicle, setNewVehicle] = useState({
    brand: "",
    model: "",
    plate: "",
    color: "",
    type: "passenger",
  });

  const handleAddVehicle = () => {
    if (newVehicle.brand && newVehicle.model && newVehicle.plate) {
      setVehicles([
        ...vehicles,
        {
          id: vehicles.length + 1,
          ...newVehicle,
        },
      ]);
      setNewVehicle({
        brand: "",
        model: "",
        plate: "",
        color: "",
        type: "passenger",
      });
      setIsDialogOpen(false);
    }
  };

  const handleDeleteVehicle = (id: number) => {
    setVehicles(vehicles.filter((v) => v.id !== id));
  };

  return (
    <div className="min-h-screen w-full bg-gray-50 pb-8">
      {/* Header */}
      <div className="bg-gradient-to-b from-[#0A1929] to-[#152C42] rounded-b-3xl pb-8 px-6 pt-12">
        <button
          onClick={() => navigate("/profile")}
          className="flex items-center gap-2 text-[#D4AF37] mb-6"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Voltar</span>
        </button>
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-white mb-2">
              Meus Veículos
            </h1>
            <p className="text-gray-400">Gerir os seus veículos</p>
          </div>
          <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
            <DialogTrigger asChild>
              <Button className="h-12 w-12 rounded-full bg-[#D4AF37] hover:bg-[#B8982E] text-[#0A1929] p-0">
                <Plus className="w-6 h-6" />
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-md">
              <DialogHeader>
                <DialogTitle>Adicionar Veículo</DialogTitle>
              </DialogHeader>
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <Label htmlFor="brand">Marca *</Label>
                  <Input
                    id="brand"
                    placeholder="BMW"
                    value={newVehicle.brand}
                    onChange={(e) =>
                      setNewVehicle({ ...newVehicle, brand: e.target.value })
                    }
                    className="h-12 rounded-xl"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="model">Modelo *</Label>
                  <Input
                    id="model"
                    placeholder="320d"
                    value={newVehicle.model}
                    onChange={(e) =>
                      setNewVehicle({ ...newVehicle, model: e.target.value })
                    }
                    className="h-12 rounded-xl"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="plate">Matrícula *</Label>
                  <Input
                    id="plate"
                    placeholder="AA-00-BB"
                    value={newVehicle.plate}
                    onChange={(e) =>
                      setNewVehicle({ ...newVehicle, plate: e.target.value })
                    }
                    className="h-12 rounded-xl"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="color">Cor</Label>
                  <Input
                    id="color"
                    placeholder="Preto"
                    value={newVehicle.color}
                    onChange={(e) =>
                      setNewVehicle({ ...newVehicle, color: e.target.value })
                    }
                    className="h-12 rounded-xl"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="type">Tipo *</Label>
                  <Select
                    value={newVehicle.type}
                    onValueChange={(value) =>
                      setNewVehicle({ ...newVehicle, type: value })
                    }
                  >
                    <SelectTrigger className="h-12 rounded-xl">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="passenger">Passageiros</SelectItem>
                      <SelectItem value="suv">SUV</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <Button
                  onClick={handleAddVehicle}
                  className="w-full h-12 bg-[#D4AF37] hover:bg-[#B8982E] text-[#0A1929] rounded-xl"
                >
                  Adicionar Veículo
                </Button>
              </div>
            </DialogContent>
          </Dialog>
        </div>
      </div>

      <div className="px-6 -mt-4 space-y-3">
        {vehicles.map((vehicle) => (
          <Card key={vehicle.id} className="p-5 border-none shadow-lg">
            <div className="flex gap-4">
              <div className="w-16 h-16 bg-[#D4AF37]/10 rounded-2xl flex items-center justify-center flex-shrink-0">
                <Car className="w-8 h-8 text-[#D4AF37]" />
              </div>
              <div className="flex-1">
                <div className="flex items-start justify-between mb-2">
                  <div>
                    <h3 className="font-bold text-[#0A1929]">
                      {vehicle.brand} {vehicle.model}
                    </h3>
                    <p className="text-sm text-gray-600 font-mono">
                      {vehicle.plate}
                    </p>
                  </div>
                  <Badge
                    className={`${
                      vehicle.type === "passenger"
                        ? "bg-blue-100 text-blue-700"
                        : "bg-purple-100 text-purple-700"
                    } border-none`}
                  >
                    {vehicle.type === "passenger" ? "Passageiros" : "SUV"}
                  </Badge>
                </div>
                {vehicle.color && (
                  <p className="text-sm text-gray-600 mb-3">Cor: {vehicle.color}</p>
                )}
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    className="text-[#0A1929] border-[#0A1929] hover:bg-[#0A1929] hover:text-white rounded-lg"
                  >
                    <Edit className="w-4 h-4 mr-1" />
                    Editar
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleDeleteVehicle(vehicle.id)}
                    className="text-red-600 border-red-600 hover:bg-red-600 hover:text-white rounded-lg"
                  >
                    <Trash2 className="w-4 h-4 mr-1" />
                    Remover
                  </Button>
                </div>
              </div>
            </div>
          </Card>
        ))}

        {vehicles.length === 0 && (
          <Card className="p-12 border-none shadow-lg text-center">
            <Car className="w-16 h-16 text-gray-400 mx-auto mb-4" />
            <h3 className="font-semibold text-[#0A1929] mb-2">
              Nenhum veículo registado
            </h3>
            <p className="text-sm text-gray-600 mb-6">
              Adicione os seus veículos para facilitar futuras marcações
            </p>
            <Button
              onClick={() => setIsDialogOpen(true)}
              className="bg-[#D4AF37] hover:bg-[#B8982E] text-[#0A1929] rounded-xl"
            >
              <Plus className="w-5 h-5 mr-2" />
              Adicionar Veículo
            </Button>
          </Card>
        )}
      </div>
    </div>
  );
}
