  # Load necessary libraries
  library(arrow)   # For reading Parquet files
  library(ggplot2) # For plotting
  
  # File path for the Battery Parquet file
  
  # Read the Parquet file
  battery_data <- read_parquet("battery.parquet")
  
  # Convert the timestamp column to a POSIXct format for plotting
  battery_data$timestamp <- as.POSIXct(battery_data$timestamp / 1000, origin = "1970-01-01", tz = "UTC")
  
  # Plot Power Draw over Time
  power_draw_plot <- ggplot(battery_data, aes(x = timestamp, y = power_draw)) +
    geom_line(color = "blue", size = 1) +  # Line for power draw
    labs(
      title = "Power Draw Over Time",
      x = "Timestamp",
      y = "Power Draw (Units)"
    ) +
    theme_minimal() +
    theme(
      plot.title = element_text(hjust = 0.5),
      axis.text.x = element_text(angle = 45, hjust = 1)
    )
  
  # Plot Charge Level over Time
  charge_level_plot <- ggplot(battery_data, aes(x = timestamp, y = charge_level)) +
    geom_line(color = "green", size = 1) +  # Line for charge level
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
  
  # Plot Power Usage over Time
  power_usage_plot <- ggplot(battery_data, aes(x = timestamp, y = energy_usage)) +
    geom_line(color = "purple", size = 1) +  # Line for power usage
    labs(
      title = "Power Usage Over Time",
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
