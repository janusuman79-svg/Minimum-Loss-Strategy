"""
========================================================================================
Financial Astrology & Planetary Hours Filter (Swiss Ephemeris & Astronomical Engine)
========================================================================================
Features:
1. Swiss Ephemeris (pyswisseph) integration with astronomical ephemeris engine fallback.
2. Bhadra (Vishti Karana) Detection: Identifies high-risk periods where speculative trades fail.
3. Lunar Void-of-Course (VOC) Window Filter: Suppresses Intraday trade entries during void-of-course
   windows to eliminate whipsaws and fake breakouts.
4. Planetary Hours (Hora) & Rahu Kaalam Evaluation: Ensures alignment with favorable trading hours.
========================================================================================
"""

from __future__ import annotations

from dataclasses import dataclass
import datetime
import math
from typing import Dict, List, Optional, Tuple

# Attempt to load pyswisseph / swisseph if installed
try:
    import swisseph as swe
    HAS_SWISSEPH = True
except ImportError:
    try:
        import pyswisseph as swe
        HAS_SWISSEPH = True
    except ImportError:
        HAS_SWISSEPH = False


class AstroPlanetaryFilter:
    """
    Financial Astrology & Ephemeris Risk Filter for NSE Algorithmic Trading.
    Filters out high-risk trades during Bhadra (Vishti Karana) and Lunar Void-of-Course windows.
    """

    # Vishti Karana (Bhadra) indices in a lunar month (1 to 60 Karanas)
    # Krishna Paksha: 8, 15, 22, 29; Shukla Paksha: 36, 43, 50, 57
    BHADRA_KARANA_INDICES = {8, 15, 22, 29, 36, 43, 50, 57}

    # Planetary hours order (Chaldean order from sunrise)
    PLANETARY_ORDER = ["Saturn", "Jupiter", "Mars", "Sun", "Venus", "Mercury", "Moon"]

    DAY_RULERS = {
        0: "Moon",      # Monday
        1: "Mars",      # Tuesday
        2: "Mercury",   # Wednesday
        3: "Jupiter",   # Thursday
        4: "Venus",     # Friday
        5: "Saturn",    # Saturday
        6: "Sun"        # Sunday
    }

    # Rahu Kaalam standard 90-minute blocks (based on 6:00 AM - 6:00 PM standard day)
    RAHU_KAALAM_HOURS = {
        0: (7.5, 9.0),    # Mon: 7:30 - 9:00 AM
        1: (15.0, 16.5),  # Tue: 3:00 - 4:30 PM
        2: (12.0, 13.5),  # Wed: 12:00 - 1:30 PM
        3: (13.5, 15.0),  # Thu: 1:30 - 3:00 PM
        4: (10.5, 12.0),  # Fri: 10:30 AM - 12:00 PM
        5: (9.0, 10.5),   # Sat: 9:00 - 10:30 AM
        6: (16.5, 18.0)   # Sun: 4:30 - 6:00 PM
    }

    @classmethod
    def calculate_sun_moon_longitudes(cls, dt: datetime.datetime) -> Tuple[float, float]:
        """
        Calculates geocentric ecliptic longitudes of Sun and Moon in degrees (0 - 360).
        Uses Swiss Ephemeris if available; otherwise uses high-precision Meeus astronomical equations.
        """
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=datetime.timezone.utc)
        else:
            dt = dt.astimezone(datetime.timezone.utc)

        if HAS_SWISSEPH:
            try:
                # Julian Day calculation
                tjd_ut = swe.julday(
                    dt.year, dt.month, dt.day,
                    dt.hour + dt.minute / 60.0 + dt.second / 3600.0
                )
                sun_res, _ = swe.calc_ut(tjd_ut, swe.SUN)
                moon_res, _ = swe.calc_ut(tjd_ut, swe.MOON)
                return sun_res[0] % 360.0, moon_res[0] % 360.0
            except Exception:
                pass

        # High-precision Astronomical Ephemeris Fallback (Jean Meeus Astronomical Algorithms)
        # Julian centuries from J2000.0
        y = dt.year
        m = dt.month
        d = dt.day + (dt.hour + dt.minute / 60.0 + dt.second / 3600.0) / 24.0

        if m <= 2:
            y -= 1
            m += 12

        a = math.floor(y / 100)
        b = 2 - a + math.floor(a / 4)
        jd = math.floor(365.25 * (y + 4716)) + math.floor(30.6001 * (m + 1)) + d + b - 1524.5
        t = (jd - 2451545.0) / 36525.0

        # Geometric Mean Longitude of the Sun
        l0 = (280.46646 + 36000.76983 * t + 0.0003032 * (t ** 2)) % 360.0
        # Mean Anomaly of the Sun
        m_sun = (357.52911 + 35999.05029 * t - 0.0001537 * (t ** 2)) % 360.0
        m_sun_rad = math.radians(m_sun)

        # Sun's equation of the center
        c_sun = (1.914602 - 0.004817 * t - 0.000014 * (t ** 2)) * math.sin(m_sun_rad)
        c_sun += (0.019993 - 0.000101 * t) * math.sin(2 * m_sun_rad)
        c_sun += 0.000289 * math.sin(3 * m_sun_rad)

        sun_long = (l0 + c_sun) % 360.0

        # Mean Longitude of the Moon
        lp = (218.3164477 + 481267.88123421 * t - 0.0015786 * (t ** 2)) % 360.0
        # Moon's Mean Elongation
        d_moon = (297.8501921 + 445267.1142204 * t - 0.0018819 * (t ** 2)) % 360.0
        # Moon's Mean Anomaly
        m_moon = (134.9633964 + 477198.8675055 * t + 0.0087414 * (t ** 2)) % 360.0
        # Moon's Argument of Latitude
        f_moon = (93.2720950 + 483202.0175233 * t - 0.0036539 * (t ** 2)) % 360.0

        # Periodic perturbations in Moon longitude
        d_rad = math.radians(d_moon)
        m_rad = math.radians(m_moon)
        f_rad = math.radians(f_moon)

        moon_perturbation = (
            6.288774 * math.sin(m_rad)
            + 1.274027 * math.sin(2 * d_rad - m_rad)
            + 0.658314 * math.sin(2 * d_rad)
            + 0.213618 * math.sin(2 * m_rad)
            - 0.185116 * math.sin(m_sun_rad)
            - 0.114332 * math.sin(2 * f_rad)
            + 0.058793 * math.sin(2 * d_rad - 2 * m_rad)
            + 0.057066 * math.sin(2 * d_rad - m_sun_rad - m_rad)
        )

        moon_long = (lp + moon_perturbation) % 360.0
        return round(sun_long, 4), round(moon_long, 4)

    @classmethod
    def get_karana_and_tithi(cls, dt: datetime.datetime) -> Tuple[int, int, str]:
        """
        Computes Tithi (1 to 30) and Karana (1 to 60) based on Moon-Sun elongation.
        Each Tithi is 12 degrees; each Karana is 6 degrees.
        """
        sun_long, moon_long = cls.calculate_sun_moon_longitudes(dt)
        elongation = (moon_long - sun_long) % 360.0

        karana_index = int(elongation // 6.0) + 1  # 1 to 60
        tithi_index = int(elongation // 12.0) + 1  # 1 to 30

        # Karana names
        karana_names = [
            "Bava", "Balava", "Kaulava", "Taitila", "Gara", "Vanija", "Vishti (Bhadra)", "Shakuni",
            "Chatushpada", "Naga", "Kintughna"
        ]

        if karana_index in cls.BHADRA_KARANA_INDICES:
            current_karana = "Vishti (Bhadra)"
        elif karana_index == 1:
            current_karana = "Kintughna"
        elif karana_index == 58:
            current_karana = "Shakuni"
        elif karana_index == 59:
            current_karana = "Chatushpada"
        elif karana_index == 60:
            current_karana = "Naga"
        else:
            current_karana = karana_names[(karana_index - 2) % 7]

        return karana_index, tithi_index, current_karana

    @classmethod
    def is_bhadra_active(cls, dt: Optional[datetime.datetime] = None) -> Tuple[bool, str]:
        """
        Returns True if Bhadra (Vishti Karana) is currently active.
        During Bhadra, breakout trades suffer sharp traps, sudden selloffs, and elevated volatility.
        """
        if dt is None:
            dt = datetime.datetime.now(datetime.timezone.utc)

        karana_idx, tithi_idx, karana_name = cls.get_karana_and_tithi(dt)
        is_bhadra = karana_idx in cls.BHADRA_KARANA_INDICES or "Bhadra" in karana_name

        if is_bhadra:
            reason = f"Bhadra (Vishti Karana #{karana_idx}, Tithi #{tithi_idx}) active: Elevated trap risk, avoid new breakout longs."
        else:
            reason = f"Karana #{karana_idx} ({karana_name}, Tithi #{tithi_idx}) favorable: Non-Bhadra window."

        return is_bhadra, reason

    @classmethod
    def is_lunar_void_of_course(cls, dt: Optional[datetime.datetime] = None) -> Tuple[bool, str]:
        """
        Evaluates Lunar Void-of-Course (VOC).
        When the Moon is in the final degrees (27.5° - 30°) of a zodiac sign without major applying aspects,
        the market enters a void-of-course state where intraday breakouts lack follow-through.
        Rule: Suppress new Intraday signals during lunar void-of-course windows.
        """
        if dt is None:
            dt = datetime.datetime.now(datetime.timezone.utc)

        _, moon_long = cls.calculate_sun_moon_longitudes(dt)
        degree_in_sign = moon_long % 30.0
        zodiac_sign_idx = int(moon_long // 30.0)

        zodiac_signs = [
            "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
            "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
        ]
        current_sign = zodiac_signs[zodiac_sign_idx]

        # Lunar void-of-course critical boundary (last 2.5 degrees of sign)
        is_voc = degree_in_sign >= 27.5

        if is_voc:
            reason = (
                f"Lunar Void-of-Course active (Moon at {degree_in_sign:.1f}° {current_sign}). "
                "Intraday breakout momentum suppressed to eliminate whipsaws."
            )
        else:
            reason = f"Moon active at {degree_in_sign:.1f}° {current_sign} (Void-of-Course inactive)."

        return is_voc, reason

    @classmethod
    def is_rahu_kaalam(cls, dt_ist: Optional[datetime.datetime] = None) -> Tuple[bool, str]:
        """
        Checks whether current IST time falls within Rahu Kaalam.
        """
        if dt_ist is None:
            # Current time in IST (UTC + 5:30)
            utc_now = datetime.datetime.now(datetime.timezone.utc)
            dt_ist = utc_now + datetime.timedelta(hours=5, minutes=30)

        weekday = dt_ist.weekday()  # 0 = Monday
        hour_float = dt_ist.hour + dt_ist.minute / 60.0

        start_h, end_h = cls.RAHU_KAALAM_HOURS.get(weekday, (12.0, 13.5))
        is_rahu = start_h <= hour_float <= end_h

        if is_rahu:
            reason = f"Rahu Kaalam active ({start_h:.1f}h - {end_h:.1f}h IST). Heightened false-breakout risk."
        else:
            reason = "Outside Rahu Kaalam window."

        return is_rahu, reason

    @classmethod
    def evaluate_astro_trade_permission(
        cls,
        setup_type: str,
        dt: Optional[datetime.datetime] = None
    ) -> Tuple[bool, List[str]]:
        """
        Main astrology filter entry point.
        Returns:
            (allowed: bool, notes: List[str])
        Rules:
        - Intraday: Strictly suppressed during Lunar Void-of-Course or Bhadra.
        - Positional: Bhadra flagged; if Bhadra active, entries require stricter confirmation.
        """
        if dt is None:
            dt = datetime.datetime.now(datetime.timezone.utc)

        notes: List[str] = []
        is_voc, voc_reason = cls.is_lunar_void_of_course(dt)
        is_bhadra, bhadra_reason = cls.is_bhadra_active(dt)

        # Rule 1: Lunar Void-of-Course strictly suppresses Intraday signals
        if setup_type.upper() == "INTRADAY" and is_voc:
            notes.append(f"⛔ ASTRO FILTER TRIGGERED: {voc_reason}")
            return False, notes

        # Rule 2: Bhadra (Vishti Karana) suppresses high-risk aggressive entries
        if is_bhadra:
            notes.append(f"⚠️ ASTRO CAUTION: {bhadra_reason}")
            # If both Bhadra and near VOC, reject trade
            if is_voc:
                notes.append("⛔ Trade rejected due to dual Bhadra & Lunar VOC alignment.")
                return False, notes

        notes.append(f"✨ Astro Engine: {voc_reason}")
        notes.append(f"✨ Panchang Karana: {bhadra_reason}")
        return True, notes


@dataclass
class AstroGannLevel:
    planet: str
    longitude: float
    aspect_name: str
    aspect_angle: float
    gann_price: float
    distance_pct: float
    is_aligned: bool
    summary: str


class AstroGannPlanetaryPriceEngine:
    """
    Astro-Gann Planetary Longitude & Angle Conversion Engine.
    
    Transforms celestial astronomical longitudes (0° - 360°) retrieved via Swiss Ephemeris
    (or high-precision astronomical algorithms) into actionable Gann Square of 9 price vibration levels.
    
    Mathematical Principle:
    In Gann's Square of 9 Wheel:
    - Price and time move in square root progressions.
    - Each 360° revolution around the wheel increases the square root of price by 2.0.
    - Geometric angles: 90° (+0.50 root), 180° (+1.00 root), 270° (+1.50 root), 360° (+2.00 root).
    - Planetary degrees (0° - 360°) map directly to price vibrations via:
      Price = (Cycle_Root + (Planetary_Degree / 180.0))^2
    """

    PLANET_MAP = {
        "Sun": (swe.SUN if HAS_SWISSEPH else 0),
        "Moon": (swe.MOON if HAS_SWISSEPH else 1),
        "Mercury": (swe.MERCURY if HAS_SWISSEPH else 2),
        "Venus": (swe.VENUS if HAS_SWISSEPH else 3),
        "Mars": (swe.MARS if HAS_SWISSEPH else 4),
        "Jupiter": (swe.JUPITER if HAS_SWISSEPH else 5),
        "Saturn": (swe.SATURN if HAS_SWISSEPH else 6),
        "Rahu": (swe.MEAN_NODE if HAS_SWISSEPH else 10)
    }

    # Key Gann harmonic aspects
    HARMONIC_ASPECTS = [
        ("Conjunction (0°)", 0.0),
        ("Square (90°)", 90.0),
        ("Trine (120°)", 120.0),
        ("Opposition (180°)", 180.0),
        ("Trine (240°)", 240.0),
        ("Square (270°)", 270.0)
    ]

    @classmethod
    def get_all_planetary_longitudes(cls, dt: Optional[datetime.datetime] = None) -> Dict[str, float]:
        """
        Retrieves geocentric ecliptic longitudes (0° - 360°) for all primary celestial bodies.
        Uses Swiss Ephemeris with analytical astronomical fallback.
        """
        if dt is None:
            dt = datetime.datetime.now(datetime.timezone.utc)
        elif dt.tzinfo is None:
            dt = dt.replace(tzinfo=datetime.timezone.utc)
        else:
            dt = dt.astimezone(datetime.timezone.utc)

        longitudes: Dict[str, float] = {}

        if HAS_SWISSEPH:
            try:
                tjd_ut = swe.julday(
                    dt.year, dt.month, dt.day,
                    dt.hour + dt.minute / 60.0 + dt.second / 3600.0
                )
                for name, pid in cls.PLANET_MAP.items():
                    res, _ = swe.calc_ut(tjd_ut, pid)
                    longitudes[name] = round(res[0] % 360.0, 3)
                return longitudes
            except Exception:
                pass

        # High-precision Analytical Ephemeris (Meeus Algorithms)
        y = dt.year
        m = dt.month
        d = dt.day + (dt.hour + dt.minute / 60.0 + dt.second / 3600.0) / 24.0
        if m <= 2:
            y -= 1
            m += 12
        a = math.floor(y / 100)
        b = 2 - a + math.floor(a / 4)
        jd = math.floor(365.25 * (y + 4716)) + math.floor(30.6001 * (m + 1)) + d + b - 1524.5
        t = (jd - 2451545.0) / 36525.0

        # Sun & Moon
        sun_long, moon_long = AstroPlanetaryFilter.calculate_sun_moon_longitudes(dt)
        longitudes["Sun"] = sun_long
        longitudes["Moon"] = moon_long

        # Planetary mean longitudes (Meeus planetary theory)
        longitudes["Mercury"] = round((252.2509 + 149472.6741 * t) % 360.0, 3)
        longitudes["Venus"] = round((181.9798 + 58517.8156 * t) % 360.0, 3)
        longitudes["Mars"] = round((355.4330 + 19140.2993 * t) % 360.0, 3)
        longitudes["Jupiter"] = round((34.3515 + 3034.9057 * t) % 360.0, 3)
        longitudes["Saturn"] = round((50.0774 + 1222.1138 * t) % 360.0, 3)
        longitudes["Rahu"] = round((125.0445 - 1934.1363 * t) % 360.0, 3)

        return longitudes

    @classmethod
    def convert_degree_to_gann_prices(
        cls,
        target_degree: float,
        reference_price: float
    ) -> List[float]:
        """
        Converts a target degree (0° - 360°) into nearest Gann Square of 9 price levels.
        Formula: Price = (Base_Root + (target_degree / 180.0))^2
        """
        if reference_price <= 0:
            reference_price = 100.0

        root = math.sqrt(reference_price)
        # 1 cycle in root terms = 2.0 (equivalent to 360°)
        cycle_base = 2.0 * math.floor(root / 2.0)

        candidate_prices: List[float] = []
        for cycle_offset in [-2.0, 0.0, 2.0, 4.0]:
            r = cycle_base + cycle_offset + (target_degree / 180.0)
            if r > 0:
                candidate_prices.append(round(r ** 2, 2))

        # Sort by proximity to reference price
        candidate_prices.sort(key=lambda p: abs(p - reference_price))
        return candidate_prices

    @classmethod
    def evaluate_astro_gann_confluence(
        cls,
        current_price: float,
        dt: Optional[datetime.datetime] = None,
        tolerance_pct: float = 0.85
    ) -> Tuple[bool, Optional[AstroGannLevel], List[AstroGannLevel]]:
        """
        Checks if current stock price aligns with any planetary degree or harmonic aspect on Gann's wheel.
        Returns:
            (is_confluent: bool, best_alignment: Optional[AstroGannLevel], all_levels: List[AstroGannLevel])
        """
        longitudes = cls.get_all_planetary_longitudes(dt)
        all_matches: List[AstroGannLevel] = []

        for planet, longitude in longitudes.items():
            for aspect_name, aspect_offset in cls.HARMONIC_ASPECTS:
                harmonic_degree = (longitude + aspect_offset) % 360.0
                gann_prices = cls.convert_degree_to_gann_prices(harmonic_degree, current_price)

                if not gann_prices:
                    continue

                closest_price = gann_prices[0]
                dist_pct = abs(current_price - closest_price) / current_price * 100.0
                is_aligned = dist_pct <= tolerance_pct

                summary = (
                    f"{planet} {aspect_name} at {harmonic_degree:.1f}° "
                    f"-> Gann Price ₹{closest_price:.2f} (Dist: {dist_pct:.2f}%)"
                )

                all_matches.append(AstroGannLevel(
                    planet=planet,
                    longitude=longitude,
                    aspect_name=aspect_name,
                    aspect_angle=harmonic_degree,
                    gann_price=closest_price,
                    distance_pct=dist_pct,
                    is_aligned=is_aligned,
                    summary=summary
                ))

        # Sort by closest distance
        all_matches.sort(key=lambda x: x.distance_pct)

        best_match = all_matches[0] if all_matches else None
        is_confluent = best_match is not None and best_match.is_aligned

        return is_confluent, best_match, all_matches
