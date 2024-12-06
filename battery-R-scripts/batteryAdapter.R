# Load necessary libraries
library(arrow)   # For reading Parquet files
library(ggplot2) # For plotting
library(tidyr)   # For data transformation
library(dplyr)   # For filtering data

# File paths for the Parquet files
file_path_energy <- "batteryAdapter.parquet"
file_path_carbon <- "powerSource.parquet"

# Read the Parquet files
energy_data <- read_parquet(file_path_energy)
carbon_data <- read_parquet(file_path_carbon)

# Convert timestamp columns to POSIXct format
energy_data$timestamp <- as.POSIXct(energy_data$timestamp / 1000, origin = "1970-01-01", tz = "UTC")
carbon_data$timestamp <- as.POSIXct(carbon_data$timestamp / 1000, origin = "1970-01-01", tz = "UTC")

# Toggle for switching between first month, first year, or entire dataset
use_first_month <- TRUE
use_first_year <- FALSE

# Apply filtering logic based on the toggles for the energy data
filtered_energy_data <- if (use_first_month) {
  energy_data %>%
    filter(format(timestamp, "%Y-%m") == format(min(timestamp), "%Y-%m"))
} else if (use_first_year) {
  energy_data %>%
    filter(format(timestamp, "%Y") == format(min(timestamp), "%Y"))
} else {
  energy_data
}

# Apply the same filtering for the carbon data
filtered_carbon_data <- if (use_first_month) {
  carbon_data %>%
    filter(format(timestamp, "%Y-%m") == format(min(timestamp), "%Y-%m"))
} else if (use_first_year) {
  carbon_data %>%
    filter(format(timestamp, "%Y") == format(min(timestamp), "%Y"))
} else {
  carbon_data
}

# Merge the energy data with the carbon intensity data on timestamp
merged_data <- filtered_energy_data %>%
  left_join(filtered_carbon_data, by = "timestamp")

# Transform the energy data to long format for easier plotting
data_long <- merged_data %>%
  pivot_longer(
    cols = c(energy_usage, energy_usage_battery, energy_usage_power_source),
    names_to = "energy_type",
    values_to = "energy_value"
  )

# Create the combined plot with energy usage and carbon intensity
combined_plot <- ggplot(data_long, aes(x = timestamp)) +
  # Energy usage lines
  geom_line(aes(y = energy_value, color = energy_type), size = 1) +
  
  # Carbon intensity line
  geom_line(data = merged_data, aes(y = carbon_intensity, color = "Carbon Intensity"), size = 1, linetype = "dashed") +
  
  # Customize colors
  scale_color_manual(
    values = c(
      "energy_usage" = "blue",
      "energy_usage_battery" = "green",
      "energy_usage_power_source" = "purple",
      "Carbon Intensity" = "red"
    ),
    labels = c(
      "Total Energy Usage",
      "Battery Energy Usage",
      "Power Source Energy Usage",
      "Carbon Intensity"
    )
  ) +
  
  # Add labels and themes
  labs(
    title = if (use_first_month) "Energy Usage and Carbon Intensity (First Month)" else if (use_first_year) "Energy Usage and Carbon Intensity (First Year)" else "Energy Usage and Carbon Intensity (Entire Duration)",
    x = "Timestamp",
    y = "Value (Units)",
    color = "Legend"
  ) +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5),
    axis.text.x = element_text(angle = 45, hjust = 1)
  )

# Print the combined plot
print(combined_plot)
