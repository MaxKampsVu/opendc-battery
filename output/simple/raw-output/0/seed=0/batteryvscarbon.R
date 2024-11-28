# Load necessary libraries
library(arrow)   # For reading Parquet files
library(ggplot2) # For plotting

# File paths for the Parquet files
file_path_main <- "powerSource.parquet"       # Main data file
file_path_battery <- "battery.parquet" # Battery data file

# Read the Parquet files
data_main <- read_parquet(file_path_main)
data_battery <- read_parquet(file_path_battery)

# Convert timestamps to a more interpretable format (if needed)
data_main$timestamp <- as.POSIXct(data_main$timestamp / 1000, origin = "1970-01-01", tz = "UTC")
data_battery$timestamp <- as.POSIXct(data_battery$timestamp / 1000, origin = "1970-01-01", tz = "UTC")

# Merge the two datasets on the timestamp column
merged_data <- merge(data_main, data_battery, by = "timestamp", suffixes = c("_main", "_battery"))

# Create the main power draw and carbon intensity plot
main_plot <- ggplot(merged_data, aes(x = timestamp)) +
  # Plot the red "Carbon Intensity" line last to bring it to the foreground
  geom_line(aes(y = power_draw_main, color = "Main Power Draw")) +
  geom_line(aes(y = power_draw_battery, color = "Battery Power Draw")) +
  geom_line(aes(y = carbon_intensity, color = "Carbon Intensity"), size = 1.5) +  # Thicker red line
  
  # Add a dotted horizontal line at y = 100 with a label
  geom_hline(yintercept = 100, linetype = "dotted", color = "black") +
  annotate("text", x = min(merged_data$timestamp), y = 100, label = "Carbon Cut Off", 
           color = "black", hjust = -0.1, vjust = -0.5) +
  
  # Manual color settings for legend and lines
  scale_color_manual(
    values = c(
      "Main Power Draw" = "blue",
      "Carbon Intensity" = "red",
      "Battery Power Draw" = "green"
    ),
    name = "Legend"
  ) +
  labs(
    title = "Power Draw and Carbon Intensity Over Time",
    x = "Timestamp",
    y = "Values"
  ) +
  theme_minimal() +
  theme(legend.position = "top")

# Create the charge level plot
charge_plot <- ggplot(data_battery, aes(x = timestamp, y = charge_level)) +
  geom_line(color = "purple", size = 1) +
  labs(
    title = "Battery Charge Level Over Time",
    x = "Timestamp",
    y = "Charge Level (%)"
  ) +
  theme_minimal() +
  theme(
    plot.title = element_text(hjust = 0.5),
    axis.text.x = element_text(angle = 45, hjust = 1)
  )

# Print both plots
print(main_plot)
#print(charge_plot)
