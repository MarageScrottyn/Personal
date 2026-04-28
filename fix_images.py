import sqlite3
conn = sqlite3.connect('db.sqlite3')
cursor = conn.cursor()
cursor.execute('UPDATE api_comicchapter SET images = "[]" WHERE images IS NULL')
conn.commit()
print('Updated', cursor.rowcount, 'rows')
conn.close()
