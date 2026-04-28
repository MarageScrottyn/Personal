import sqlite3
conn = sqlite3.connect('db.sqlite3')
cur = conn.cursor()
cur.execute('SELECT id, comic_id, images FROM api_comicchapter')
print('ComicChapter table:')
for row in cur.fetchall():
    print(f'  id={row[0]}, comic_id={row[1]}, images={row[2]}')
conn.close()
