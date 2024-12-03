# Load necessary libraries
library(arrow)   # For reading Parquet files
library(ggplot2) # For plotting

# File path for the Parquet file
file_path <- "powerSource.parquet"

# Read the Parquet file
data <- read_parquet(file_path)

# Convert the timestamp column to a POSIXct format for plotting
data$timestamp <- as.POSIXct(data$timestamp / 1000, origin = "1970-01-01", tz = "UTC")

# Plot the energy usage data with battery usage in the foreground
energy_usage_plot <- ggplot(data, aes(x = timestamp)) +
  
  # First plot the total energy usage (background)
  geom_line(aes(y = energy_usage, color = "Total Energy Usage"), size = 0.8) +
  
  # Plot the adapter usage (middle layer)
  geom_line(aes(y = energy_usage_adapter, color = "Adapter Usage"), size = 0.8) +
  
  # Plot the battery usage (foreground)
  geom_line(aes(y = energy_usage_battery, color = "Battery Usage"), size = 1.2) +
  
  # Customize the legend colors
  scale_color_manual(
    values = c(
      "Total Energy Usage" = "blue",
      "Battery Usage" = "green",
      "Adapter Usage" = "orange"
    ),
    name = "Legend"
  ) +
  
  # Add plot labels
  labs(
    title = "Energy Usage: Total, Battery, and Adapter",
    x = "Timestamp",
    y = "Energy Usage (Watts)"
  ) +
  
  # Apply a clean theme
  theme_minimal() +
  theme(
    legend.position = "top",
    axis.text.x = element_text(angle = 45, hjust = 1),
    plot.title = element_text(hjust = 0.5)
  )

# Print the energy usage plot
print(energy_usage_plot)

# Carbon Intensity Plot
carbon_intensity_plot <- ggplot(data, aes(x = timestamp, y = carbon_intensity)) +
  geom_line(color = "red", size = 1) +  # Line for carbon intensity
  labs(
    title = "Carbon Intensity Over Time",
    x = "Timestamp",
    y = "Carbon Intensity (Units)"
  ) +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5),
    axis.text.x = element_text(angle = 45, hjust = 1)
  )

# Carbon Emission Plot
carbon_emission_plot <- ggplot(data, aes(x = timestamp)) +
  geom_line(aes(y = carbon_emission), color = "red", size = 1.2) +
  labs(
    title = "Carbon Emission Over Time",
    x = "Timestamp",
    y = "Carbon Emission (grams)"
  ) +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5),
    axis.text.x = element_text(angle = 45, hjust = 1)
  )

# Power Draw Plot
power_draw_plot <- ggplot(data, aes(x = timestamp, y = power_draw)) +
  geom_line(color = "blue", size = 1) +  # Line for power draw
  labs(
    title = "Power Draw Over Time (PowerSource)",
    x = "Timestamp",
    y = "Power Draw (Units)"
  ) +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5),
    axis.text.x = element_text(angle = 45, hjust = 1)
  )

# Print additional plots
print(carbon_intensity_plot)
print(carbon_emission_plot)
print(power_draw_plot)
