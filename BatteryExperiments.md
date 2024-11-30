# Run an experiment with a Battery 

## Set up:

1.) Set the `maximum capacity` and `charge current` of the battery in the file`HostsProvisioningStep.kt`

2.) Set the `carbonIntensityThreshold` in the file `SimpleCarbonPolicy.java` 

2.) Add a carbon trace to the folder `carbon_traces` and add the path in `HostsProvisioningStep.kt`

3.) (Optional) Add a new experiment to `resources` and add the path in `ExperimentCli.kt`. The standard basic experiment will probably suffice for our analysis 

4.) Run the simulation 

## Evaluate Outputs

1.) The outputs are in `output/simple/raw-output/{0,1}` (the simulation is run twice)

2.) The `BatteryAdapter`, `SimBattery` and `SimPowerSource`are logged in `batteryAdapter.parquet`, `battery.parquet`and `powerSource.parquet` respectively

3.) (Optional) In the folder `battery-R-scripts` are some R scripts to evaluate these files. You have to set the working directory 
with `setwd("path-to-parquet-file")` in R Studio  


## Some pointers when setting parameters for the Battery 

### Battery depletion 
After you run a simulation, check if the charge level of the battery is negative or not, adjust values accordingly. 
The simulator updates every 30000000ms (approximately 8 hours) and supplied power is computed after the next time step. 
Consequently, in this time frame we can not check if the battery was depleted or not. I have found no easy way to fix this. 
The solution I propose is to make the capacity of the battery large enough so it never fully depletes. 

### Easy to read carbon traces 
In the folder `carbon_traces` is a file `sin_carbon_trace` which simply logs a sin wave. It allows to visualize the switch between power supply and battery more clearly 

