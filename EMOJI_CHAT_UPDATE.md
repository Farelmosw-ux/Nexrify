# Chat Emoji Update — Nexrify 4.0.7

- Synchronized Android `CHAT_EMOJIS` with the full server whitelist from `Nexrify-Web-API/api/_chat.js`.
- Total unique emoji in the picker: 604.
- Reactions animate with a spring scale response when selected.
- Quick reactions remain available; the expanded picker exposes the full list.

## Platform emoji appearance
The app currently renders Unicode emoji through the Android system emoji font. Android apps cannot force Apple's proprietary iOS emoji artwork without distributing licensed Apple assets. Therefore this update does not falsely claim pixel-identical iPhone emoji; appearance may vary by Android OS/vendor. The animation added here is the reaction interaction animation, not Telegram's proprietary animated emoji sticker pack.
