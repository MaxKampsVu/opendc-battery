# Load necessary libraries
library(arrow)   # For reading Parquet files
library(ggplot2) # For plotting
library(tidyr)   # For data transformation
library(dplyr)   # For filtering data

# File path for the Parquet file
file_path <- "batteryAdapter.parquet"

# Read the Parquet file
data <- read_parquet(file_path)

# Convert the timestamp column to a POSIXct format for plotting
data$timestamp <- as.POSIXct(data$timestamp / 1000, origin = "1970-01-01", tz = "UTC")

# Toggle for switching between first month, first year, or entire dataset
use_first_month <- FALSE
use_first_year <- TRUE


# Apply filtering logic based on the toggles
filtered_data <- if (use_first_month) {
  data %>%
    filter(format(timestamp, "%Y-%m") == format(min(timestamp), "%Y-%m"))
} else if (use_first_year) {
  data %>%
    filter(format(timestamp, "%Y") == format(min(timestamp), "%Y"))
} else {
  data
}

# ----- Plot 1: Energy Usages Over Time -----

# Transform the data to long format for easier plotting with ggplot
data_long <- filtered_data %>%
  pivot_longer(
    cols = c(energy_usage, energy_usage_battery, energy_usage_power_source),
    names_to = "energy_type",
    values_to = "energy_value"
  )

# Create the energy usage plot
energy_usage_plot <- ggplot(data_long, aes(x = timestamp, y = energy_value, color = energy_type)) +
  geom_line(size = 1) +
  scale_color_manual(
    values = c(
      "energy_usage" = "blue",
      "energy_usage_battery" = "green",
      "energy_usage_power_source" = "purple"
    ),
    labels = c(
      "Total Energy Usage",
      "Battery Energy Usage",
      "Power Source Energy Usage"
    )
  ) +
  labs(
    title = if (use_first_month) "Energy Usages (First Month)" else if (use_first_year) "Energy Usages (First Year)" else "Energy Usages (Entire Duration)",
    x = "Timestamp",
    y = "Energy Usage (Units)",
    color = "Energy Type"
  ) +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5),
    axis.text.x = element_text(angle = 45, hjust = 1)
  )

# ----- Plot 2: Power Draw Over Time -----

# Create the power draw plot
power_draw_plot <- ggplot(filtered_data, aes(x = timestamp, y = power_draw)) +
  geom_line(color = "red", size = 1) +
  labs(
    title = if (use_first_month) "Power Draw (First Month)" else if (use_first_year) "Power Draw (First Year)" else "Power Draw (Entire Duration)",
    x = "Timestamp",
    y = "Power Draw (Units)"
  ) +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5),
    axis.text.x = element_text(angle = 45, hjust = 1)
  )

# Print the plots
print(energy_usage_plot)
#print(power_draw_plot)
