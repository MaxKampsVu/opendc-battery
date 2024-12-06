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

# Toggle to choose between first month, first year, or entire dataset
use_first_month <- TRUE
use_first_year <- FALSE

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

# Calculate the cumulative carbon emission
filtered_data <- filtered_data %>%
  arrange(timestamp) %>%  # Ensure the data is sorted by timestamp
  mutate(cumulative_carbon_emission = cumsum(carbon_emission))

# Energy Usage Plot
energy_usage_plot <- ggplot(filtered_data, aes(x = timestamp)) +
  
  # Total energy usage (background)
  geom_line(aes(y = energy_usage, color = "Total Energy Usage"), size = 0.8) +
  
  # Adapter usage (middle layer)
  geom_line(aes(y = energy_usage_adapter, color = "Adapter Usage"), size = 0.8) +
  
  # Battery usage (foreground)
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
    title = if (use_first_month) "Energy Usage (First Month)" else if (use_first_year) "Energy Usage (First Year)" else "Energy Usage (Entire Duration)",
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

# Carbon Intensity Plot
carbon_intensity_plot <- ggplot(filtered_data, aes(x = timestamp, y = carbon_intensity)) +
  geom_hline(yintercept = 100, linetype = "dashed", color = "blue", size = 1) +  
  geom_line(color = "red", size = 1) +  # Line for carbon intensity
  labs(
    title = if (use_first_month) "Carbon Intensity (First Month)" else if (use_first_year) "Carbon Intensity (First Year)" else "Carbon Intensity (Entire Duration)",
    x = "Timestamp",
    y = "Carbon Intensity (Units)"
  ) +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5),
    axis.text.x = element_text(angle = 45, hjust = 1)
  )

# Cumulative Carbon Emission Plot
cumulative_carbon_emission_plot <- ggplot(filtered_data, aes(x = timestamp, y = cumulative_carbon_emission)) +
  geom_line(color = "purple", size = 1.2) +  # Line for cumulative carbon emission
  labs(
    title = if (use_first_month) "Cumulative Carbon Emission (First Month)" else if (use_first_year) "Cumulative Carbon Emission (First Year)" else "Cumulative Carbon Emission (Entire Duration)",
    x = "Timestamp",
    y = "Cumulative Carbon Emission (grams)"
  ) +
  scale_y_continuous(
    limits = c(0, max(filtered_data$cumulative_carbon_emission, na.rm = TRUE) * 1.1),  # Set scale dynamically
    expand = c(0, 0)        # Prevent extra space around the plot
  ) +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5),
    axis.text.x = element_text(angle = 45, hjust = 1)
  )

# Power Draw Plot
power_draw_plot <- ggplot(filtered_data, aes(x = timestamp, y = power_draw)) +
  geom_line(color = "darkred", size = 1) +
  labs(
    title = if (use_first_month) "Power Draw (First Month)" else if (use_first_year) "Power Draw (First Year)" else "Power Draw (Entire Duration)",
    x = "Timestamp",
    y = "Power Draw (Watts)"
  ) +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5),
    axis.text.x = element_text(angle = 45, hjust = 1)
  )



# Print the plots
print(energy_usage_plot)
print(carbon_intensity_plot)
print(cumulative_carbon_emission_plot)
print(power_draw_plot)
