CREATE TABLE address (
  id UUID PRIMARY KEY,
  street1 TEXT NOT NULL,
  street2 TEXT,
  street3 TEXT,
  city TEXT NOT NULL,
  zip TEXT NOT NULL,
  country TEXT NOT NULL
);

CREATE TABLE person (
  id UUID PRIMARY KEY,
  title TEXT NOT NULL,
  firstname TEXT NOT NULL,
  lastname TEXT NOT NULL,
  "birthDate" DATE,
  "addressId" UUID REFERENCES address(id)
);
