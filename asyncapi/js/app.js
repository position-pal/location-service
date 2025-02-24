
    const schema = {
  "asyncapi": "3.0.0",
  "info": {
    "title": "Location Events API",
    "version": "1.0.0",
    "description": "API for publishing location-based tracking events in the PositionPal system.\nThis WebSocket API allows real-time communication for location tracking, routing, and SOS alerts.\nClients can send various events and receive user updates from the server.\n",
    "contact": {
      "email": "positionpal@gmail.com",
      "url": "https://position-pal.github.io"
    },
    "tags": [
      {
        "name": "real-time"
      },
      {
        "name": "location"
      },
      {
        "name": "tracking"
      }
    ]
  },
  "servers": {
    "production": {
      "host": "api.positionpal.com",
      "protocol": "ws",
      "protocolVersion": "1.0",
      "bindings": {
        "ws": {
          "bindingVersion": "1.0.0",
          "method": "GET"
        }
      }
    }
  },
  "channels": {
    "group/{groupId}/{userId}": {
      "description": "Main WebSocket channel for all location-based events.\nClients can send location updates, routing events, and SOS alerts.\nThe server will send user status updates through this channel.\n",
      "parameters": {
        "groupId": {
          "description": "the id of the group"
        },
        "userId": {
          "description": "the id of the user"
        }
      },
      "messages": {
        "SampledLocation": {
          "title": "Sampled Location Update",
          "summary": "Regular location update from a user",
          "contentType": "application/json",
          "payload": {
            "type": "object",
            "properties": {
              "SampledLocation": {
                "type": "object",
                "required": [
                  "timestamp",
                  "user",
                  "group",
                  "position"
                ],
                "properties": {
                  "timestamp": {
                    "type": "string",
                    "format": "date-time",
                    "x-parser-schema-id": "<anonymous-schema-4>"
                  },
                  "user": {
                    "type": "string",
                    "description": "Unique identifier for a user.",
                    "x-parser-schema-id": "UserId"
                  },
                  "group": {
                    "type": "string",
                    "description": "Unique identifier for a group.",
                    "x-parser-schema-id": "GroupId"
                  },
                  "position": {
                    "type": "object",
                    "required": [
                      "latitude",
                      "longitude"
                    ],
                    "properties": {
                      "latitude": {
                        "type": "number",
                        "format": "float",
                        "minimum": -90,
                        "maximum": 90,
                        "x-parser-schema-id": "<anonymous-schema-5>"
                      },
                      "longitude": {
                        "type": "number",
                        "format": "float",
                        "minimum": -180,
                        "maximum": 180,
                        "x-parser-schema-id": "<anonymous-schema-6>"
                      }
                    },
                    "x-parser-schema-id": "GPSLocation"
                  }
                },
                "x-parser-schema-id": "<anonymous-schema-3>"
              }
            },
            "x-parser-schema-id": "SampledLocation"
          },
          "x-parser-unique-object-id": "SampledLocation",
          "x-parser-message-name": "SampledLocation"
        },
        "RoutingStarted": {
          "title": "Routing Started Event",
          "summary": "Event when a user starts navigation",
          "contentType": "application/json",
          "payload": {
            "type": "object",
            "properties": {
              "RoutingStarted": {
                "type": "object",
                "required": [
                  "timestamp",
                  "user",
                  "group",
                  "position",
                  "mode",
                  "destination",
                  "expectedArrival"
                ],
                "properties": {
                  "timestamp": {
                    "type": "string",
                    "format": "date-time",
                    "x-parser-schema-id": "<anonymous-schema-8>"
                  },
                  "user": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.user",
                  "group": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.group",
                  "position": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.position",
                  "mode": {
                    "type": "string",
                    "enum": [
                      "WALKING",
                      "DRIVING",
                      "CYCLING"
                    ],
                    "x-parser-schema-id": "RoutingMode"
                  },
                  "destination": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.position",
                  "expectedArrival": {
                    "type": "string",
                    "format": "date-time",
                    "x-parser-schema-id": "<anonymous-schema-9>"
                  }
                },
                "x-parser-schema-id": "<anonymous-schema-7>"
              }
            },
            "x-parser-schema-id": "RoutingStarted"
          },
          "x-parser-unique-object-id": "RoutingStarted",
          "x-parser-message-name": "RoutingStarted"
        },
        "RoutingStopped": {
          "title": "Routing Stopped Event",
          "summary": "Event when a user stops navigation",
          "contentType": "application/json",
          "payload": {
            "type": "object",
            "properties": {
              "RoutingStopped": {
                "type": "object",
                "required": [
                  "timestamp",
                  "user",
                  "group"
                ],
                "properties": {
                  "timestamp": {
                    "type": "string",
                    "format": "date-time",
                    "x-parser-schema-id": "<anonymous-schema-11>"
                  },
                  "user": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.user",
                  "group": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.group"
                },
                "x-parser-schema-id": "<anonymous-schema-10>"
              }
            },
            "x-parser-schema-id": "RoutingStopped"
          },
          "x-parser-unique-object-id": "RoutingStopped",
          "x-parser-message-name": "RoutingStopped"
        },
        "SOSAlertTriggered": {
          "title": "SOS Alert Triggered",
          "summary": "Emergency alert triggered by user",
          "contentType": "application/json",
          "payload": {
            "type": "object",
            "properties": {
              "SOSAlertTriggered": {
                "type": "object",
                "required": [
                  "timestamp",
                  "user",
                  "group",
                  "position"
                ],
                "properties": {
                  "timestamp": {
                    "type": "string",
                    "format": "date-time",
                    "x-parser-schema-id": "<anonymous-schema-13>"
                  },
                  "user": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.user",
                  "group": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.group",
                  "position": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.position"
                },
                "x-parser-schema-id": "<anonymous-schema-12>"
              }
            },
            "x-parser-schema-id": "SOSAlertTriggered"
          },
          "x-parser-unique-object-id": "SOSAlertTriggered",
          "x-parser-message-name": "SOSAlertTriggered"
        },
        "SOSAlertStopped": {
          "title": "SOS Alert Stopped",
          "summary": "Emergency alert cancelled by user",
          "contentType": "application/json",
          "payload": {
            "type": "object",
            "properties": {
              "SOSAlertStopped": {
                "type": "object",
                "required": [
                  "timestamp",
                  "user",
                  "group"
                ],
                "properties": {
                  "timestamp": {
                    "type": "string",
                    "format": "date-time",
                    "x-parser-schema-id": "<anonymous-schema-15>"
                  },
                  "user": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.user",
                  "group": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.group"
                },
                "x-parser-schema-id": "<anonymous-schema-14>"
              }
            },
            "x-parser-schema-id": "SOSAlertStopped"
          },
          "x-parser-unique-object-id": "SOSAlertStopped",
          "x-parser-message-name": "SOSAlertStopped"
        },
        "UserUpdate": {
          "title": "User Status Update",
          "summary": "Server-sent update about a user's status",
          "contentType": "application/json",
          "payload": {
            "type": "object",
            "properties": {
              "UserUpdate": {
                "type": "object",
                "required": [
                  "timestamp",
                  "user",
                  "group",
                  "position",
                  "status"
                ],
                "properties": {
                  "timestamp": {
                    "type": "string",
                    "format": "date-time",
                    "x-parser-schema-id": "<anonymous-schema-17>"
                  },
                  "user": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.user",
                  "group": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.group",
                  "position": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.position",
                  "status": {
                    "description": "The current state of a user in the system",
                    "enum": [
                      "ACTIVE",
                      "INACTIVE",
                      "ROUTING",
                      "WARNING",
                      "SOS"
                    ],
                    "x-parser-schema-id": "UserState"
                  }
                },
                "x-parser-schema-id": "<anonymous-schema-16>"
              }
            },
            "x-parser-schema-id": "UserUpdate"
          },
          "x-parser-unique-object-id": "UserUpdate",
          "x-parser-message-name": "UserUpdate"
        }
      },
      "x-parser-unique-object-id": "group/{groupId}/{userId}"
    }
  },
  "operations": {
    "onSampledLocation": {
      "action": "send",
      "channel": "$ref:$.channels.group/{groupId}/{userId}",
      "summary": "Send sampled location updates to the server.",
      "messages": [
        "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation"
      ],
      "x-parser-unique-object-id": "onSampledLocation"
    },
    "onRoutingStarted": {
      "action": "send",
      "channel": "$ref:$.channels.group/{groupId}/{userId}",
      "summary": "Send routing start events to the server.",
      "messages": [
        "$ref:$.channels.group/{groupId}/{userId}.messages.RoutingStarted"
      ],
      "x-parser-unique-object-id": "onRoutingStarted"
    },
    "onRoutingStopped": {
      "action": "send",
      "channel": "$ref:$.channels.group/{groupId}/{userId}",
      "summary": "Send routing stop events to the server.",
      "messages": [
        "$ref:$.channels.group/{groupId}/{userId}.messages.RoutingStopped"
      ],
      "x-parser-unique-object-id": "onRoutingStopped"
    },
    "onSOSAlertTriggered": {
      "action": "send",
      "channel": "$ref:$.channels.group/{groupId}/{userId}",
      "summary": "Send SOS alerts to the server.",
      "messages": [
        "$ref:$.channels.group/{groupId}/{userId}.messages.SOSAlertTriggered"
      ],
      "x-parser-unique-object-id": "onSOSAlertTriggered"
    },
    "onSOSAlertStopped": {
      "action": "send",
      "channel": "$ref:$.channels.group/{groupId}/{userId}",
      "summary": "Send SOS alert stops to the server.",
      "messages": [
        "$ref:$.channels.group/{groupId}/{userId}.messages.SOSAlertStopped"
      ],
      "x-parser-unique-object-id": "onSOSAlertStopped"
    },
    "onUserUpdate": {
      "action": "receive",
      "channel": "$ref:$.channels.group/{groupId}/{userId}",
      "summary": "Receive user update events from the server.",
      "messages": [
        "$ref:$.channels.group/{groupId}/{userId}.messages.UserUpdate"
      ],
      "x-parser-unique-object-id": "onUserUpdate"
    }
  },
  "components": {
    "securitySchemes": {
      "bearerAuth": {
        "type": "http",
        "scheme": "bearer",
        "bearerFormat": "JWT"
      }
    },
    "parameters": {
      "userId": "$ref:$.channels.group/{groupId}/{userId}.parameters.userId",
      "groupId": "$ref:$.channels.group/{groupId}/{userId}.parameters.groupId"
    },
    "schemas": {
      "UserId": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.user",
      "GroupId": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.group",
      "GPSLocation": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload.properties.SampledLocation.properties.position",
      "RoutingMode": "$ref:$.channels.group/{groupId}/{userId}.messages.RoutingStarted.payload.properties.RoutingStarted.properties.mode",
      "UserState": "$ref:$.channels.group/{groupId}/{userId}.messages.UserUpdate.payload.properties.UserUpdate.properties.status",
      "SampledLocation": "$ref:$.channels.group/{groupId}/{userId}.messages.SampledLocation.payload",
      "RoutingStarted": "$ref:$.channels.group/{groupId}/{userId}.messages.RoutingStarted.payload",
      "RoutingStopped": "$ref:$.channels.group/{groupId}/{userId}.messages.RoutingStopped.payload",
      "SOSAlertTriggered": "$ref:$.channels.group/{groupId}/{userId}.messages.SOSAlertTriggered.payload",
      "SOSAlertStopped": "$ref:$.channels.group/{groupId}/{userId}.messages.SOSAlertStopped.payload",
      "UserUpdate": "$ref:$.channels.group/{groupId}/{userId}.messages.UserUpdate.payload"
    }
  },
  "x-parser-spec-parsed": true,
  "x-parser-api-version": 3,
  "x-parser-spec-stringified": true
};
    const config = {"show":{"sidebar":true},"sidebar":{"showOperations":"byDefault"}};
    const appRoot = document.getElementById('root');
    AsyncApiStandalone.render(
        { schema, config, }, appRoot
    );
  