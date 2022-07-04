import telebot

TOKEN = "5209962730:AAH4gPaaCQ964ekyAwcTGbDMjKyIOwHfzB0"
bot = telebot.TeleBot(TOKEN)


@bot.message_handler(commands=['start'])
def start_message(msg):
    dr_id = msg.chat.id
    dr_name = msg.chat.first_name
    buttons = telebot.types.ReplyKeyboardMarkup(True)
    buttons.row('ID')
    bot.send_message(dr_id, f'Hello Dr. {dr_name}\nYour ID: {dr_id}\nPlease send it to your patient.', reply_markup=buttons)


@bot.message_handler(commands=['id'])
def start_message(msg):
    dr_id = msg.chat.id
    buttons = telebot.types.ReplyKeyboardMarkup(True)
    buttons.row('ID')
    bot.send_message(dr_id, dr_id, reply_markup=buttons)


@bot.message_handler(content_types=['text'])
def send_text(msg):
    if msg.text.lower() == 'id':
        dr_id = msg.chat.id
        bot.send_message(dr_id, dr_id)


bot.polling()