CREATE TABLE city (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100)
);

CREATE TABLE theatre (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    city_id INT REFERENCES city(id)
);

CREATE TABLE movie (
    id SERIAL PRIMARY KEY,
    title VARCHAR(100),
    genre VARCHAR(50),
    language VARCHAR(50)
);

CREATE TABLE show (
    id SERIAL PRIMARY KEY,
    movie_id INT REFERENCES movie(id),
    theatre_id INT REFERENCES theatre(id),
    show_date DATE,
    show_time TIME,
    price DOUBLE PRECISION
);

CREATE TABLE seat (
    id SERIAL PRIMARY KEY,
    show_id INT REFERENCES show(id),
    seat_number VARCHAR(10),
    is_booked BOOLEAN DEFAULT FALSE,
    UNIQUE (show_id, seat_number)
);

CREATE TABLE booking (
    id SERIAL PRIMARY KEY,
    show_id INT REFERENCES show(id),
    total_price DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE booking_seat (
    booking_id INT REFERENCES booking(id),
    seat_id INT REFERENCES seat(id)
);
