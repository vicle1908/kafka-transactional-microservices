#!/bin/bash

# outbox-replay.sh - Script to replay outbox messages
# Usage: ./scripts/outbox-replay.sh [--message-id <id>] [--time-range <start> <end>] [--service <service-name>]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

HELP_TEXT="Usage: $0 [OPTIONS]

Replay outbox messages for testing and recovery purposes.

Options:
  --message-id <id>        Replay a specific message by ID
  --time-range <start> <end>  Replay messages within a time range (ISO format)
  --service <name>         Replay messages for a specific service
  --help                   Show this help message

Examples:
  $0 --message-id 123e4567-e89b-12d3-a456-426614174000
  $0 --time-range 2025-10-01T00:00:00 2025-10-01T01:00:00
  $0 --service orders
"

# Default values
MESSAGE_ID=""
START_TIME=""
END_TIME=""
SERVICE_NAME=""

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --message-id)
      MESSAGE_ID="$2"
      shift 2
      ;;
    --time-range)
      START_TIME="$2"
      END_TIME="$3"
      shift 3
      ;;
    --service)
      SERVICE_NAME="$2"
      shift 2
      ;;
    --help)
      echo "$HELP_TEXT"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "$HELP_TEXT"
      exit 1
      ;;
  esac
done

# Validate inputs
if [[ -n "$MESSAGE_ID" && (-n "$START_TIME" || -n "$END_TIME" || -n "$SERVICE_NAME") ]]; then
  echo "Error: --message-id cannot be used with other options"
  exit 1
fi

if [[ (-n "$START_TIME" || -n "$END_TIME") && (-n "$MESSAGE_ID" || -n "$SERVICE_NAME") ]]; then
  echo "Error: --time-range cannot be used with --message-id or --service"
  exit 1
fi

if [[ (-n "$SERVICE_NAME") && (-n "$MESSAGE_ID" || -n "$START_TIME" || -n "$END_TIME") ]]; then
  echo "Error: --service cannot be used with other options"
  exit 1
fi

if [[ -n "$START_TIME" && -z "$END_TIME" ]] || [[ -z "$START_TIME" && -n "$END_TIME" ]]; then
  echo "Error: --time-range requires both start and end times"
  exit 1
fi

# Function to replay a specific message
replay_message() {
  local message_id="$1"
  echo "Replaying message with ID: $message_id"
  
  # In a real implementation, this would make an API call to the service
  # curl -X POST "http://localhost:8080/api/outbox/replay/$message_id"
  echo "Would make API call to replay message: $message_id"
}

# Function to replay messages in a time range
replay_time_range() {
  local start_time="$1"
  local end_time="$2"
  echo "Replaying messages from $start_time to $end_time"
  
  # In a real implementation, this would make an API call to the service
  # curl -X POST "http://localhost:8080/api/outbox/replay-range?startTime=${start_time}&endTime=${end_time}"
  echo "Would make API call to replay messages in time range: $start_time to $end_time"
}

# Function to replay messages for a service
replay_service() {
  local service_name="$1"
  echo "Replaying messages for service: $service_name"
  
  # In a real implementation, this would make an API call to the service
  # curl -X POST "http://localhost:8080/api/outbox/process"
  echo "Would make API call to process pending messages for service: $service_name"
}

# Main execution
if [[ -n "$MESSAGE_ID" ]]; then
  replay_message "$MESSAGE_ID"
elif [[ -n "$START_TIME" && -n "$END_TIME" ]]; then
  replay_time_range "$START_TIME" "$END_TIME"
elif [[ -n "$SERVICE_NAME" ]]; then
  replay_service "$SERVICE_NAME"
else
  echo "Error: No action specified"
  echo "$HELP_TEXT"
  exit 1
fi

echo "Replay operation completed"