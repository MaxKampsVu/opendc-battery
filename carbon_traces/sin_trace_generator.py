import numpy as np
import pandas as pd
import pyarrow as pa
import pyarrow.parquet as pq

# Parameters for the sinusoidal function
start_time = "2012-12-31T23:00:00Z"  # Start timestamp
end_time = "2016-12-30T22:45:00Z"    # End timestamp
interval_minutes = 15                # Time interval in minutes
amplitude_hill = 150                 # Amplitude for peaks
amplitude_valley = 50                # Amplitude for troughs
periods = 12                         # Number of complete periods

# Generate the timestamps
timestamps = pd.date_range(
    start=start_time,
    end=end_time,
    freq=f"{interval_minutes}T",
    tz="UTC"
)

# Generate the smooth sine function values
n_points = len(timestamps)
x = np.linspace(0, 2 * np.pi * periods, n_points)  # x values for the sine function

# Scale sine values to create hills and valleys
carbon_intensity = ((amplitude_hill - amplitude_valley) / 2) * np.sin(x) + \
                   ((amplitude_hill + amplitude_valley) / 2)

# Create the DataFrame
df = pd.DataFrame({
    "timestamp": timestamps,
    "carbon_intensity": carbon_intensity
})

# Convert to Parquet
schema = pa.schema([
    pa.field("timestamp", pa.timestamp("ms")),
    pa.field("carbon_intensity", pa.float64())
])
table = pa.Table.from_pandas(df, schema=schema)

# Save to a Parquet file
output_file = "sin_carbon_trace.parquet"
pq.write_table(table, output_file)

print(f"Parquet file '{output_file}' has been created.")
