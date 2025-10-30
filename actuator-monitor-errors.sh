#!/bin/bash

LOG_FILE="logs/rooms.log"
MINUTES_AGO=5

# Get the search pattern (just date + hour + minute)
PATTERN=$(date -d "$MINUTES_AGO minutes ago" '+%Y-%m-%d %H:%M')

echo "Monitoring errors since $PATTERN..."
echo "Current time: $(date)"

# Search for lines that START with the timestamp pattern
ERROR_LINES=$(grep "^$PATTERN" "$LOG_FILE" | grep "ERROR")

# Count lines properly
if [ -z "$ERROR_LINES" ]; then
    ERROR_COUNT=0
else
    ERROR_COUNT=$(echo "$ERROR_LINES" | wc -l)
fi

echo "Errors in the last $MINUTES_AGO minutes: $ERROR_COUNT"

if [ $ERROR_COUNT -gt 5 ]; then
    echo "🚨 HIGH ERROR COUNT: $ERROR_COUNT errors detected"

    # Send to Slack/email/notification
    if [ ! -z "$SLACK_WEBHOOK_URL" ]; then
        curl -X POST -H 'Content-type: application/json' \
             --data "{\"text\":\"🚨 Rooms Service: $ERROR_COUNT errors in last 5 minutes\"}" \
             "$SLACK_WEBHOOK_URL"
        echo "Alert sent to Slack"
    else
        echo "SLACK_WEBHOOK_URL not set, skipping notification"
    fi
else
    echo "✅ Error count within acceptable limits"
fi
