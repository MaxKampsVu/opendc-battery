# Load necessary libraries
library(ggplot2)
library(dplyr)
library(tidyr)
library(arrow)

# Load the Parquet file
battery_adapter_data <- read_parquet("batteryAdapter.parquet")

# Convert `timestamp` to POSIXct (milliseconds to seconds)
battery_adapter_data <- battery_adapter_data %>%
  mutate(timestamp = as.POSIXct(timestamp / 1000, origin = "1970-01-01", tz = "UTC"))

# Convert the timestamp to days since the start of the dataset
battery_adapter_data <- battery_adapter_data %>%
  mutate(timestamp_days = as.numeric(difftime(timestamp, min(timestamp), units = "days")))

# Transform the data to long format for easier plotting with ggplot
data_long <- battery_adapter_data %>%
  pivot_longer(
    cols = c(energy_usage, energy_usage_battery, energy_usage_adapter),
    names_to = "energy_type",
    values_to = "energy_value"
  )

# Create the energy usage plot
energy_usage_plot <- ggplot(data_long, aes(x = timestamp_days, y = energy_value, color = energy_type)) +
  geom_line(size = 1) +
  scale_color_manual(
    values = c(
      "energy_usage" = "black",
      "energy_usage_battery" = "green",
      "energy_usage_adapter" = "blue"
    ),
    labels = c(
      "Power Source (Energy Usage)",
      "Battery Energy (Power Adapter)",
      "Adapter Energy (Power Adapter)"
    )
  ) +
  labs(
    title = "Energy Usage for Battery Adapter",
    x = "Time (in days)",
    y = "Energy Usage (J)",
    color = "Energy Source"
  ) +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5),
    axis.text.x = element_text(angle = 45, hjust = 1)
  )

# Print the energy usage plot
print(energy_usage_plot)
