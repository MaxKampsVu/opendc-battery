# Load necessary libraries
library(arrow)   # For reading Parquet files
library(ggplot2) # For plotting
library(dplyr)   # For filtering data

# File path for the Parquet file
file_path <- "powerSource.parquet"

# Read the Parquet file
data <- read_parquet(file_path)

# Convert the timestamp column to a POSIXct format for plotting
data$timestamp <- as.POSIXct(data$timestamp / 1000, origin = "1970-01-01", tz = "UTC")

# Filter the data to include only the first month
data_first_month <- data %>%
  filter(format(timestamp, "%Y-%m") == format(min(timestamp), "%Y-%m"))

# Calculate the cumulative carbon emission
data_first_month <- data_first_month %>%
  arrange(timestamp) %>%  # Ensure the data is sorted by timestamp
  mutate(cumulative_carbon_emission = cumsum(carbon_emission))

# Plot the energy usage data with battery usage in the foreground
energy_usage_plot <- ggplot(data_first_month, aes(x = timestamp)) +
  
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

# Carbon Intensity Plot
carbon_intensity_plot <- ggplot(data_first_month, aes(x = timestamp, y = carbon_intensity)) +
  geom_hline(yintercept = 100, linetype = "dashed", color = "blue", size = 1) +  
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
carbon_emission_plot <- ggplot(data_first_month, aes(x = timestamp)) +
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

# Cumulative Carbon Emission Plot
cumulative_carbon_emission_plot <- ggplot(data_first_month, aes(x = timestamp, y = cumulative_carbon_emission)) +
  geom_line(color = "purple", size = 1.2) +  # Line for cumulative carbon emission
  labs(
    title = "Cumulative Carbon Emission Over Time",
    x = "Timestamp",
    y = "Cumulative Carbon Emission (grams)"
  ) +
  scale_y_continuous(
    limits = c(0, 150000),  # Set scale to go from 0 to 150,000
    expand = c(0, 0)        # Prevent extra space around the plot
  ) +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5),
    axis.text.x = element_text(angle = 45, hjust = 1)
  )

(energy_usage_plot)
print(carbon_emission_plot)
print(carbon_intensity_plot)
print(cumulative_carbon_emission_plot)