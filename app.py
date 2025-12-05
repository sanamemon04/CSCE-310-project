from flask import Flask, request, jsonify
from flask_bcrypt import Bcrypt
from flask_jwt_extended import JWTManager, create_access_token, jwt_required, get_jwt_identity, get_jwt
import mysql.connector
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import os
import logging
from dotenv import load_dotenv

load_dotenv()
app = Flask(__name__)
app.config['JWT_SECRET_KEY'] = 'super-secret-key'

bcrypt = Bcrypt(app)
jwt = JWTManager(app)

# -------------------------------------
# Database Connection
# -------------------------------------
db = mysql.connector.connect(
    host="localhost",
    user="root",
    password="012604",
    database="bookstore"
)
cursor = db.cursor(dictionary=True)

# -------------------------------------
# Registration
# -------------------------------------
@app.post('/register')
def register():
    data = request.json
    username = data['username']
    pw_hash = bcrypt.generate_password_hash(data['password']).decode('utf-8')
    email = data['email']
    userType = data['userType']
    
    cursor.execute("""
        INSERT INTO Account (username, passwordHash, email, userType)
        VALUES (%s, %s, %s, %s)
    """, (username, pw_hash, email, userType))
    db.commit()
    return jsonify({"message": "User registered"}), 201

# -------------------------------------
# Login
# -------------------------------------
@app.post('/login')
def login():
    data = request.json
    username = data['username']
    password = data['password']

    cursor.execute("SELECT * FROM Account WHERE username=%s", (username,))
    user = cursor.fetchone()

    if not user or not bcrypt.check_password_hash(user['passwordHash'], password):
        return jsonify({"error": "Invalid credentials"}), 401

    token = create_access_token(
        identity=str(user["userID"]),
        additional_claims={"userType": user["userType"]}
    )

    return jsonify({"token": token})

# -------------------------------------
# Search Books
# -------------------------------------
@app.get('/books/search')
@jwt_required()
def search_books():
    q = request.args.get("q", "")
    if not q:
        return jsonify([])  # return empty list if no query

    keyword = f"%{q}%"
    cursor.execute("""
        SELECT * FROM Book
        WHERE title LIKE %s OR author LIKE %s
    """, (keyword, keyword))
    rows = cursor.fetchall()

    # Normalize isAvailable -> True/False so Gson on the Java side parses booleans, not numbers
    for r in rows:
        r['isAvailable'] = bool(r.get('isAvailable'))

    return jsonify(rows)

# -------------------------------------
# Place Order (Buy or Rent)
# -------------------------------------
@app.post('/order')
@jwt_required()
def place_order():
    userID = int(get_jwt_identity())
    claims = get_jwt()
    userType = claims["userType"]

    if userType != "customer":
        return jsonify({"error": "Managers cannot place orders"}), 403
    
    data = request.json or {}
    items = data.get("items", [])

    if not items:
        return jsonify({"error": "No items in order"}), 400

    # Validate all items first (no DB modification yet)
    validated = []  # tuples (bookID, transactionType, price, title)
    totalAmount = 0.0

    for item in items:
        if "bookID" not in item or "action" not in item:
            return jsonify({"error": "Each item must contain bookID and action"}), 400

        cursor.execute("SELECT * FROM Book WHERE bookID=%s", (item["bookID"],))
        book = cursor.fetchone()
        if not book:
            return jsonify({"error": f"BookID {item['bookID']} not exist"}), 400

        # Normalize MySQL boolean/int -> Python bool
        if not book.get("isAvailable"):
            return jsonify({"error": f"BookID {item['bookID']} is not available"}), 400
        
        action = item.get("action")
        if action == "buy":
            transactionType = "buy"
            price = float(book["buyPrice"])
        elif action == "rent":
            transactionType = "rent"
            price = float(book["rentPrice"])
        else:
            return jsonify({"error": f"No valid action for bookID {item['bookID']}"}), 400

        validated.append((item["bookID"], transactionType, price, book.get("title")))
        totalAmount += price
    # All items validated -> perform inserts in one transaction
    try:
        cursor.execute(
            "INSERT INTO Orders (userID, totalAmount, paymentStatus) VALUES (%s, %s, 'Pending')",
            (userID, totalAmount)
        )
        orderID = cursor.lastrowid

        bill_items = []
        for bookID, transactionType, price, title in validated:
            cursor.execute("""
                INSERT INTO OrderItems (orderID, bookID, transactionType, price)
                VALUES (%s, %s, %s, %s)
            """, (orderID, bookID, transactionType, price))

            # Mark as unavailable after any purchase/rent so manager can re-enable later
            cursor.execute("UPDATE Book SET isAvailable=0 WHERE bookID=%s", (bookID,))

            bill_items.append({
                "title": title,
                "transactionType": transactionType,
                "price": price
            })

        db.commit()
    except Exception as e:
        db.rollback()
        app.logger.exception("Failed to create order")
        return jsonify({"error": "Failed to create order"}), 500

    # items = request.json.get("items", [])

    # if not items:
    #     return jsonify({"error": "No items in order"}), 400

    # # Insert new order
    # cursor.execute(
    #     "INSERT INTO Orders (userID, totalAmount, paymentStatus) VALUES (%s, 0, 'Pending')",
    #     (userID,)
    # )
    # db.commit()
    # orderID = cursor.lastrowid

    # totalAmount = 0
    # bill_items = []

    # for item in items:
    #     # require bookID and action
    #     if "bookID" not in item or "action" not in item:
    #         return jsonify({"error": "Each item must contain bookID and action"}), 400

    #     cursor.execute("SELECT * FROM Book WHERE bookID=%s", (item["bookID"],))
    #     book = cursor.fetchone()
    #     if not book:
    #         return jsonify({"error": f"BookID {item['bookID']} not exist"}), 400
        
    #      # Check availability (MySQL may return 0/1 or False/True)
    #     if not book["isAvailable"]:
    #         return jsonify({"error": f"BookID {item['bookID']} is not available"}), 400

    #     # transactionType = "buy" if item.get("buy") else "rent"
    #     # price = book["buyPrice"] if transactionType == "buy" else book["rentPrice"]

    #     # if book["isAvailable"] == 0:
    #     #     return jsonify({"error": f"BookID {item['bookID']} is not available"}), 400

    #     # Determine action
    #     action = item.get("action")
    #     if action == "buy":
    #         transactionType = "buy"
    #         price = book["buyPrice"]
    #     elif action == "rent":
    #         transactionType = "rent"
    #         price = book["rentPrice"]
    #     else:
    #         return jsonify({"error": f"No valid action for bookID {item['bookID']}"}), 400
    #     # if item.get("buy"):
    #     #     transactionType = "buy"
    #     #     price = book["buyPrice"]
    #     # elif item.get("rent"):
    #     #     transactionType = "rent"
    #     #     price = book["rentPrice"]
    #     # else:
    #     #     return jsonify({"error": f"No valid transaction type for bookID {item['bookID']}"}), 400

    #     totalAmount += price

    #     # Insert order item
    #     cursor.execute("""
    #         INSERT INTO OrderItems (orderID, bookID, transactionType, price)
    #         VALUES (%s, %s, %s, %s)
    #     """, (orderID, item["bookID"], transactionType, price))
        
    #     db.commit()

    #     bill_items.append({
    #         "title": book["title"],
    #         "transactionType": transactionType,
    #         "price": price
    #     })

    #     # Mark as unavailable if rented
        
    #     cursor.execute("UPDATE Book SET isAvailable=0 WHERE bookID=%s", (item["bookID"],))

    # # Update total in Orders table
    # cursor.execute("UPDATE Orders SET totalAmount=%s WHERE orderID=%s", (totalAmount, orderID))
    # db.commit()

    # Send email
    try:
        # fetch registered customer email from DB (per-order, not from env)
        cursor.execute("SELECT email FROM Account WHERE userID=%s", (userID,))
        row = cursor.fetchone()
        email = row.get("email") if row else None

        app.logger.info("Placing order %s for user %s, email=%s", orderID, userID, email)

        bill_text = f"Order ID: {orderID}\n\nItems:\n"
        for b in bill_items:
            bill_text += f"{b['title']} ({b['transactionType']}): ${b['price']}\n"
        bill_text += f"\nTotal Amount: ${totalAmount}"

        if email:
            email_ok = send_email(email, f"Your Bookstore Bill - Order {orderID}", bill_text)
            if not email_ok:
                app.logger.warning("Order %s created but email failed to send to %s", orderID, email)
        else:
            app.logger.warning("No email found for userID %s — order %s created without sending email", userID, orderID)
    except Exception as e:
        app.logger.error("Email sending encountered error: %s", e)

    return jsonify({
        "orderID": orderID,
        "totalAmount": totalAmount,
        "bill": bill_items
    })
# -------------------------------------
# Email Function (SMTP)
# -------------------------------------
def send_email(to_email, subject, body):
    # load credentials from env vars instead of hardcoding
    smtp_server = os.environ.get("SMTP_SERVER", "smtp.gmail.com")
    smtp_port = int(os.environ.get("SMTP_PORT", "587"))
    smtp_user = os.environ.get("SMTP_USER")
    smtp_password = os.environ.get("SMTP_PASSWORD")

    msg = MIMEMultipart()
    msg["From"] = smtp_user
    msg["To"] = to_email
    msg["Subject"] = subject
    msg.attach(MIMEText(body, "plain"))

    try:
        server = smtplib.SMTP(smtp_server, smtp_port, timeout=10)
        server.ehlo()
        server.starttls()
        server.login(smtp_user, smtp_password)
        server.send_message(msg)
        server.quit()
        return True
    except smtplib.SMTPAuthenticationError as e:
        logging.error("SMTP auth failed: %s", e)
        return False
    except Exception as e:
        logging.error("SMTP send failed: %s", e)
        return False

# -------------------------------------
# Manager: View All Orders
# -------------------------------------
@app.get('/orders/all')
@jwt_required()
def all_orders():
    claims = get_jwt()
    if claims["userType"] != "manager":
        return jsonify({"error": "Only managers can view all orders"}), 403

    cursor.execute("SELECT * FROM Orders")
    orders = cursor.fetchall()
    # attach items for each order (include book title)
    for o in orders:
        item_cursor = db.cursor(dictionary=True)
        try:
            item_cursor.execute("""
                SELECT oi.itemID, oi.bookID, oi.transactionType, oi.price, b.title
                FROM OrderItems oi
                LEFT JOIN Book b ON oi.bookID = b.bookID
                WHERE oi.orderID = %s
            """, (o['orderID'],))
            items = item_cursor.fetchall() or []
        finally:
            item_cursor.close()

        # normalize & ensure fields are present for JSON client
        for it in items:
            try:
                it['price'] = float(it.get('price') or 0.0)
            except Exception:
                it['price'] = 0.0
            if not it.get('transactionType'):
                it['transactionType'] = 'buy'
            if 'title' not in it:
                it['title'] = None

        o['items'] = items
        app.logger.debug("Order %s -> %d items", o.get('orderID'), len(items))

    return jsonify(orders)
          
# -------------------------------------
# Manager: Update Payment Status
# -------------------------------------
@app.post('/orders/status')
@jwt_required()
def update_payment():
    claims = get_jwt()
    if claims["userType"] != "manager":
        return jsonify({"error": "Only managers can update payment status"}), 403

    orderID = request.json["orderID"]
    new_status = request.json["paymentStatus"]

    cursor.execute("UPDATE Orders SET paymentStatus=%s WHERE orderID=%s", (new_status, orderID))
    db.commit()

    return jsonify({"message": "Status updated"})

# -------------------------------------
# Manager: All Books (Inventory)
# -------------------------------------
@app.get('/books/all')
@jwt_required()
def all_books():
    # allow managers (and customers if you want) to fetch full inventory
    # restrict to manager if desired:
    claims = get_jwt()
    # if claims["userType"] != "manager":
    #     return jsonify({"error": "Only managers can view all books"}), 403

    cursor.execute("SELECT * FROM Book")
    rows = cursor.fetchall()
    # normalize isAvailable to real boolean so Java Gson maps correctly
    for r in rows:
        r['isAvailable'] = bool(r.get('isAvailable'))
    return jsonify(rows)

# -------------------------------------
# Manager: Add Book
# -------------------------------------
@app.post('/books/add')
@jwt_required()
def add_book():
    claims = get_jwt()
    if claims["userType"] != "manager":
        return jsonify({"error": "Only managers can add books"}), 403

    data = request.json
    cursor.execute("""
        INSERT INTO Book (title, author, buyPrice, rentPrice, isAvailable)
        VALUES (%s, %s, %s, %s, %s)
    """, (
        data["title"], data["author"],
        data["buyPrice"], data["rentPrice"],
        data.get("isAvailable", 1)
    ))
    db.commit()
    return jsonify({"message": "Book added", "bookID": cursor.lastrowid})

# -------------------------------------
# Manager: Update Book Availability
# -------------------------------------
@app.post('/books/availability')
@jwt_required()
def update_book_availability():
    claims = get_jwt()
    if claims["userType"] != "manager":
        return jsonify({"error": "Only managers can update book availability"}), 403

    data = request.json
    cursor.execute(
        "UPDATE Book SET isAvailable=%s WHERE bookID=%s",
        (data["isAvailable"], data["bookID"])
    )
    db.commit()
    return jsonify({"message": "Availability updated"})

# ---------- New: User's own orders ----------
@app.get('/orders/my')
@jwt_required()
def my_orders():
    userID = int(get_jwt_identity())
    cursor.execute("SELECT * FROM Orders WHERE userID=%s", (userID,))
    orders = cursor.fetchall() or []

    for o in orders:
        item_cursor = db.cursor(dictionary=True)
        try:
            item_cursor.execute("""
                SELECT oi.itemID, oi.bookID, oi.transactionType, oi.price, b.title
                FROM OrderItems oi
                LEFT JOIN Book b ON oi.bookID = b.bookID
                WHERE oi.orderID = %s
            """, (o['orderID'],))
            items = item_cursor.fetchall() or []
        finally:
            item_cursor.close()

        for it in items:
            try:
                it['price'] = float(it.get('price') or 0.0)
            except Exception:
                it['price'] = 0.0
            if not it.get('transactionType'):
                it['transactionType'] = 'buy'
            if 'title' not in it:
                it['title'] = None
        
        o['items'] = items

    return jsonify(orders)

# GET reviews for a book
@app.get('/books/<int:book_id>/reviews')
def get_book_reviews(book_id):
    cur = db.cursor(dictionary=True)
    try:
        cur.execute("""
            SELECT r.reviewID, r.userID, a.username, r.rating, r.reviewText, r.createdAt
            FROM Reviews r
            LEFT JOIN Account a ON r.userID = a.userID
            WHERE r.bookID = %s
            ORDER BY r.createdAt DESC
        """, (book_id,))
        reviews = cur.fetchall() or []
        # ensure serializable types
        for r in reviews:
            if 'rating' in r:
                r['rating'] = int(r['rating'])
        return jsonify(reviews)
    finally:
        cur.close()
        
# POST a review (auth required)
@app.post('/books/<int:book_id>/reviews')
@jwt_required()
def post_book_review(book_id):
    data = request.get_json() or {}
    rating = data.get('rating')
    text = data.get('text', '')
    if not isinstance(rating, int) or not (1 <= rating <= 5):
        return jsonify({"msg": "rating must be int 1-5"}), 400
    user_id = int(get_jwt_identity())
    cur = db.cursor()
    try:
        cur.execute("INSERT INTO Reviews (userID, bookID, rating, reviewText) VALUES (%s,%s,%s,%s)",
                    (user_id, book_id, rating, text))
        db.commit()
        review_id = cur.lastrowid
        # return created review object
        rcur = db.cursor(dictionary=True)
        try:
            rcur.execute("SELECT r.reviewID, r.userID, a.username, r.rating, r.reviewText, r.createdAt FROM Reviews r LEFT JOIN Account a ON r.userID=a.userID WHERE r.reviewID=%s", (review_id,))
            created = rcur.fetchone()
            if created and 'rating' in created:
                created['rating'] = int(created['rating'])
            return jsonify(created), 201
        finally:
            rcur.close()
    finally:
        cur.close()

# -------------------------------------
# Run Server
# -------------------------------------
if __name__ == "__main__":
    app.run(debug=True)
