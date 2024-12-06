  # Load necessary libraries
  library(arrow)   # For reading Parquet files
  library(ggplot2) # For plotting
  library(dplyr)   # For filtering data
  
  # File path for the Battery Parquet file
  battery_data <- read_parquet("battery.parquet")
  
  # Convert the timestamp column to a POSIXct format for plotting
  battery_data$timestamp <- as.POSIXct(battery_data$timestamp / 1000, origin = "1970-01-01", tz = "UTC")
  
  # Toggle for switching between first month and entire dataset
  use_first_month <- FALSE
  
  # Apply filtering logic based on the toggle
  filtered_battery_data <- if (use_first_month) {
    battery_data %>%
      filter(format(timestamp, "%Y-%m") == format(min(timestamp), "%Y-%m"))
  } else {
    battery_data
  }
  
  # Plot Power Draw over Time
  power_draw_plot <- ggplot(filtered_battery_data, aes(x = timestamp, y = power_draw)) +
    geom_line(color = "blue", size = 1) +  # Line for power draw
    labs(
      title = if (use_first_month) "Power Draw " else "Power Draw (Entire Duration)",
      x = "Timestamp",
      y = "Power Draw (Units)"
    ) +
    theme_minimal() +
    theme(
      plot.title = element_text(hjust = 0.5),
      axis.text.x = element_text(angle = 45, hjust = 1)
    )
  
  # Plot Charge Level over Time
  charge_level_plot <- ggplot(filtered_battery_data, aes(x = timestamp, y = charge_level)) +
    geom_line(color = "green", size = 1) +  # Line for charge level
    labs(
      title = if (use_first_month) "Battery Charge Level " else "Battery Charge Level (Entire Duration)",
      x = "Timestamp",
      y = "Total Charge Level (in J)"
    ) +
    theme_minimal() +
    theme(
      plot.title = element_text(hjust = 0.5),
      axis.text.x = element_text(angle = 45, hjust = 1)
    )
  
  # Plot Power Usage over Time
  power_usage_plot <- ggplot(filtered_battery_data, aes(x = timestamp, y = energy_usage)) +
    geom_line(color = "purple", size = 1) +  # Line for power usage
    labs(
      title = if (use_first_month) "Power Usage " else "Power Usage (Entire Duration)",
      x = "Timestamp",
      y = "Power Usage (Units)"
    ) +
    theme_minimal() +
    theme(
      plot.title = element_text(hjust = 0.5),
      axis.text.x = element_text(angle = 45, hjust = 1)
    )
  
  # Print the plots
  print(power_draw_plot)
  print(charge_level_plot)
  print(power_usage_plot)
  
