# src/backend/app/routers/scenario.py
from fastapi import APIRouter
from fastapi.responses import StreamingResponse 
import json
from ..services.osm_generator import create_sumo_network
from ..services.simulation_runner import run_simulation

router = APIRouter()

@router.post("/generate")
async def generate_scenario(data: dict):
   async def generate():
       try:
           yield json.dumps({"progress": 20, "message": "Starting network generation..."})
           
           success, message = create_sumo_network(
               data["coordinates"]["lat"],
               data["coordinates"]["lng"], 
               data["radius"],
               {
                   "roadTypes": data["roadTypes"],
                   "vehicles": data["vehicles"],
                   "duration": data["duration"]
               }
           )

           if not success:
               yield json.dumps({"progress": 0, "message": message})
               return

           yield json.dumps({"progress": 100, "message": "Generation complete!"})

       except Exception as e:
           yield json.dumps({"progress": 0, "message": str(e)})

   return StreamingResponse(generate(), media_type="text/event-stream")

@router.post("/simulate")
async def start_simulation(data: dict):
   success = run_simulation(data["duration"])
   return {"status": "success" if success else "error"}