#!/bin/bash

echo "📊 Rooms Service Performance Analysis Report"
echo "============================================"
echo ""

# SLOW OPERATIONS ANALYSIS
echo "SLOW OPERATIONS (over 1000ms):"
echo "------------------------------"
grep "SLOW OPERATION" logs/rooms.log | \
  awk -F'SLOW OPERATION: ' '{print $2}' | \
  sort | uniq -c | sort -nr

echo ""

# SLOW API CALLS
echo "SLOW API CALLS (over 2000ms):"
echo "-----------------------------"
grep "SLOW API" logs/rooms.log | \
  awk -F'SLOW API: ' '{print $2}' | \
  sort | uniq -c | sort -nr

echo ""

# AVERAGE OPERATION TIMES
echo "AVERAGE OPERATION TIMES:"
echo "-----------------------"
echo "Room Creation: $(grep "Room creation completed" logs/rooms.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "Room Join: $(grep "Room join completed" logs/rooms.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "Room Leave: $(grep "Room leave completed" logs/rooms.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "User Kick: $(grep "User kick completed" logs/rooms.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "User Mute: $(grep "User mute completed" logs/rooms.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "Room Deletion: $(grep "Room deletion completed" logs/rooms.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "Room Retrieval: $(grep "Room retrieval completed" logs/rooms.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"
echo "Participants Retrieval: $(grep "Participants retrieval completed" logs/rooms.log | awk -F'in ' '{print $2}' | awk -F'ms' '{sum+=$1; count++} END {if(count>0) printf "%.2fms", sum/count}')"

echo ""

# API SUCCESS RATES
echo "API SUCCESS RATES:"
echo "-----------------"
TOTAL_CREATE_REQUESTS=$(grep "CREATE_ROOM request" logs/rooms.log | wc -l)
SUCCESSFUL_CREATES=$(grep "CREATE_ROOM success" logs/rooms.log | wc -l)
if [ $TOTAL_CREATE_REQUESTS -gt 0 ]; then
    CREATE_SUCCESS_RATE=$((SUCCESSFUL_CREATES * 100 / TOTAL_CREATE_REQUESTS))
    echo "Room Creation Success Rate: $CREATE_SUCCESS_RATE% ($SUCCESSFUL_CREATES/$TOTAL_CREATE_REQUESTS)"
else
    echo "No room creation attempts found"
fi

TOTAL_JOIN_REQUESTS=$(grep "JOIN_ROOM request" logs/rooms.log | wc -l)
SUCCESSFUL_JOINS=$(grep "JOIN_ROOM success" logs/rooms.log | wc -l)
if [ $TOTAL_JOIN_REQUESTS -gt 0 ]; then
    JOIN_SUCCESS_RATE=$((SUCCESSFUL_JOINS * 100 / TOTAL_JOIN_REQUESTS))
    echo "Room Join Success Rate: $JOIN_SUCCESS_RATE% ($SUCCESSFUL_JOINS/$TOTAL_JOIN_REQUESTS)"
else
    echo "No room join attempts found"
fi

TOTAL_LEAVE_REQUESTS=$(grep "LEAVE_ROOM request" logs/rooms.log | wc -l)
SUCCESSFUL_LEAVES=$(grep "LEAVE_ROOM success" logs/rooms.log | wc -l)
if [ $TOTAL_LEAVE_REQUESTS -gt 0 ]; then
    LEAVE_SUCCESS_RATE=$((SUCCESSFUL_LEAVES * 100 / TOTAL_LEAVE_REQUESTS))
    echo "Room Leave Success Rate: $LEAVE_SUCCESS_RATE% ($SUCCESSFUL_LEAVES/$TOTAL_LEAVE_REQUESTS)"
else
    echo "No room leave attempts found"
fi

echo ""

# ERROR ANALYSIS
echo "ERROR ANALYSIS:"
echo "--------------"
echo "Total Errors: $(grep "ERROR" logs/rooms.log | wc -l)"
echo "Room Not Found Errors: $(grep "Room not found" logs/rooms.log | wc -l)"
echo "User Not Found Errors: $(grep "User not found" logs/rooms.log | wc -l)"
echo "Permission Errors: $(grep "Only owner can" logs/rooms.log | wc -l)"
echo "Room Full Errors: $(grep "Room is full" logs/rooms.log | wc -l)"
echo "User Already in Room: $(grep "User already in room" logs/rooms.log | wc -l)"
echo "Invalid Password Errors: $(grep "Invalid room password" logs/rooms.log | wc -l)"

echo ""

# ROOM STATISTICS
echo "ROOM STATISTICS:"
echo "---------------"
TOTAL_ROOMS_CREATED=$(grep "CREATE_ROOM success" logs/rooms.log | wc -l)
TOTAL_JOINS=$(grep "JOIN_ROOM success" logs/rooms.log | wc -l)
TOTAL_LEAVES=$(grep "LEAVE_ROOM success" logs/rooms.log | wc -l)
TOTAL_KICKS=$(grep "KICK_USER success" logs/rooms.log | wc -l)
TOTAL_MUTES=$(grep "MUTE_USER success" logs/rooms.log | wc -l)
TOTAL_DELETIONS=$(grep "DELETE_ROOM success" logs/rooms.log | wc -l)

echo "Total Rooms Created: $TOTAL_ROOMS_CREATED"
echo "Total Room Joins: $TOTAL_JOINS"
echo "Total Room Leaves: $TOTAL_LEAVES"
echo "Total User Kicks: $TOTAL_KICKS"
echo "Total User Mutes: $TOTAL_MUTES"
echo "Total Room Deletions: $TOTAL_DELETIONS"

echo ""

# EVENT PROCESSING VOLUME
echo "EVENT PROCESSING VOLUME:"
echo "-----------------------"
echo "User Created Events: $(grep "Processing UserCreatedEvent" logs/rooms.log | wc -l)"
echo "User Updated Events: $(grep "Processing UserUpdatedEvent" logs/rooms.log | wc -l)"
echo "User Event Processing Errors: $(grep "Failed to process.*Event" logs/rooms.log | wc -l)"

echo ""

# PERFORMANCE SUMMARY
echo "PERFORMANCE SUMMARY:"
echo "-------------------"
SLOW_OPERATIONS_COUNT=$(grep -c "SLOW OPERATION" logs/rooms.log)
SLOW_API_COUNT=$(grep -c "SLOW API" logs/rooms.log)

TOTAL_SLOW_OPERATIONS=$((SLOW_OPERATIONS_COUNT + SLOW_API_COUNT))

if [ $TOTAL_SLOW_OPERATIONS -eq 0 ]; then
    echo "✅ No slow operations detected"
elif [ $TOTAL_SLOW_OPERATIONS -lt 10 ]; then
    echo "⚠️  Few slow operations: $TOTAL_SLOW_OPERATIONS total"
    echo "   - SLOW OPERATIONS: $SLOW_OPERATIONS_COUNT"
    echo "   - SLOW API: $SLOW_API_COUNT"
else
    echo "🚨 High number of slow operations: $TOTAL_SLOW_OPERATIONS total"
    echo "   - SLOW OPERATIONS: $SLOW_OPERATIONS_COUNT"
    echo "   - SLOW API: $SLOW_API_COUNT"
fi

# Calculate overall error rate
TOTAL_REQUESTS=$((TOTAL_CREATE_REQUESTS + TOTAL_JOIN_REQUESTS + TOTAL_LEAVE_REQUESTS))
TOTAL_ERRORS=$(grep -c "ERROR" logs/rooms.log)

if [ $TOTAL_REQUESTS -gt 0 ]; then
    ERROR_RATE=$((TOTAL_ERRORS * 100 / TOTAL_REQUESTS))
    echo "Overall Error Rate: $ERROR_RATE% ($TOTAL_ERRORS/$TOTAL_REQUESTS)"
fi
