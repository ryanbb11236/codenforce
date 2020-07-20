import requests
import json

import os

from collections import namedtuple

from _db_conn import get_db_and_cursor
from _constants import PARCEL_ID_LISTS


Municipality = namedtuple("Municipalicty", ["municode", "name"])


def main():
    with get_db_and_cursor() as db_cursor:
        muni_generator = munis(db_cursor)
        for muni in muni_generator:
            try:
                file_name = muni_data_and_write_to_file(muni)
            finally:
                validate_muni_json(file_name)


def munis(db_cursor, ):
    select_sql = "SELECT municode, muniname FROM municipality;"
    db_cursor.execute(select_sql)
    munis = db_cursor.fetchall()
    for row in munis:
        yield Municipality(*row)


def muniname_from_municode(municode, db_cursor):
    select_sql = "SELECT municode, muniname FROM municipality where municode = %s"
    db_cursor.execute(select_sql, [municode])
    row = db_cursor.fetchone()
    try:
        return Municipality(*row)
    except TypeError as e:
        if row is None:
            raise TypeError(
                "The municode given as an argument likely does not exist in the database"
            )
        else:
            raise e


def muni_data_and_write_to_file(Municipality):
    # Note: The WPRDC limits 50,000 parcels
    script_dir = os.path.dirname(__file__)
    rel_path = os.path.join(PARCEL_ID_LISTS, Municipality.name + "_parcelids.json")
    abs_path = os.path.join(script_dir, rel_path)

    with open(abs_path, "w") as f:
        wprdc_url = """https://data.wprdc.org/api/3/action/datastore_search_sql?sql=
        SELECT * FROM "518b583f-7cc8-4f60-94d0-174cc98310dc" WHERE "MUNICODE" = '{}'""".format(
            Municipality.municode
        )
        req = requests.get(wprdc_url)
        try:
            f.write(req.text)
        except IOError as e:
            # Todo: log_error
            raise e
        print("Written {}".format(abs_path))
    return abs_path


def prop_id(parid, db_cursor):
    select_sql = """
        SELECT propertyid FROM public.property
        WHERE parid = %s;"""
    db_cursor.execute(select_sql, [parid])
    return db_cursor.fetchone()[0]  # property id


def unit_id(prop_id, db_cursor):
    select_sql = """
        SELECT unitid FROM propertyunit
        WHERE property_propertyid = %s"""
    db_cursor.execute(select_sql, prop_id)
    try:
        return db_cursor.fetchone()[0]  # unit id
    except TypeError:
        return None

def ecase_id(self, db_cursor):
    select_sql = """
        SELECT caseid FROM cecase
        WHERE property_propertyid = %s
        ORDER BY creationtimestamp DESC;"""
    db_cursor.execute(select_sql, [self.prop_id])
    try:
        return db_cursor.fetchone()[0]  # Case ID
    except TypeError:  # 'NoneType' object is not subscriptable:
        return None





def validate_muni_json(file_name):
    with open(file_name, "r") as file:
        f = json.load(file)
        if not (f["success"] and len(f["result"]["records"]) > 0):
            # Check and see if it's a test municipality
            if file.name.startswith(os.path.join(PARCEL_ID_LISTS, "COG Land")):
                return True
            # Todo: log_error
            os.rename(file_name, file_name + "_corrupt")
            raise ValueError("{} not valid".format(file.name))
        return True
    print(file_name, "could not be validated. Skipping.")


if __name__ == "__main__":
    main()
